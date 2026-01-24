package com.example.hrmanagement.service

import com.example.hrmanagement.data.AttendanceHistoryItem
import com.example.hrmanagement.data.AttendanceTodayResponse
import com.example.hrmanagement.model.CheckInOutRequest
import com.example.hrmanagement.model.SimpleResponse
import com.example.hrmanagement.repo.AttendanceRepository
import com.example.hrmanagement.repo.QRScanRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AttendanceService(
    private val attendanceRepo: AttendanceRepository = AttendanceRepository()
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    // QR validation / cooldown configuration
    private val windowSeconds = 30L
    private val qrSkewWindows = 1                // allow ±1 window
    private val rescanCooldownSeconds = 3L * 60L // 3 minutes

    // Business rule: shift start for "late" detection (09:00)
    private val shiftStart = LocalTime.of(9, 0)

    private fun currentWindow(): Long {
        val epoch = Instant.now().epochSecond
        return epoch / windowSeconds
    }

    /**
     * Parse & validate QR payload expected in format "DEVICE_ID|WINDOW"
     * Returns Pair(deviceId, window) or throws IllegalArgumentException on failure.
     */
    private fun parseAndValidateQr(qr: String?): Pair<String, Long> {
        if (qr.isNullOrBlank()) throw IllegalArgumentException("Missing QR payload")

        val parts = qr.split("|")
        if (parts.size != 2) throw IllegalArgumentException("Invalid QR format")

        val deviceId = parts[0].trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Empty device id in QR")

        val window = parts[1].trim().toLongOrNull()
            ?: throw IllegalArgumentException("Invalid window in QR")

        val nowWindow = currentWindow()
        if (kotlin.math.abs(nowWindow - window) > qrSkewWindows) {
            throw IllegalArgumentException("QR expired or not yet valid")
        }

        return Pair(deviceId, window)
    }

    /**
     * Handle check-in / check-out request.
     * Validates QR, enforces cooldown per user, logs scan, then records attendance.
     */
    fun handleCheck(userId: Int, request: CheckInOutRequest): SimpleResponse {


        // Validate employee mapping
        val employeeId = attendanceRepo.findEmployeeIdForUser(userId)
            ?: return SimpleResponse(false, "Employee record not found for userId=$userId")
        // 🚫 Block attendance on leave days
        val today = LocalDate.now()
        val todayAttendance = attendanceRepo.getTodayAttendance(employeeId)

        if (todayAttendance?.status == "ON_LEAVE") {
            return SimpleResponse(false, "You are on leave today")
        }


        // Validate action
        val action = request.action.uppercase().trim()
        if (action != "CHECK_IN" && action != "CHECK_OUT") {
            return SimpleResponse(false, "Unknown action: $action")
        }

        // Validate QR
        val (deviceId, window) = try {
            parseAndValidateQr(request.qr)
        } catch (e: Exception) {
            return SimpleResponse(false, "QR validation failed: ${e.message}")
        }

        try {
            // ------------------------------
            // Per-user cooldown check (not per-device)
            // ------------------------------
            val lastForUser = QRScanRepository.lastScanForUser(userId)
            if (lastForUser != null) {
                val secondsSince = java.time.Duration.between(lastForUser.scannedAt, LocalDateTime.now()).seconds
                if (secondsSince < rescanCooldownSeconds) {
                    val wait = (rescanCooldownSeconds - secondsSince)
                    return SimpleResponse(false, "Please wait $wait seconds before scanning again")
                }
            }

            // ------------------------------
            // Auto-close any previous open attendance that is older than today
            // ------------------------------
            val open = attendanceRepo.findOpenAttendanceForEmployee(employeeId)
            val today = LocalDate.now()
            if (open != null) {
                if (open.date.isBefore(today)) {
                    // choose a reasonable auto-close time: checkIn + 8 hours (or now if earlier)
                    val defaultAutoClose = open.checkIn?.plusHours(8) ?: LocalDateTime.of(open.date, LocalTime.of(17, 0))
                    val autoCloseTime = if (defaultAutoClose.isAfter(LocalDateTime.now())) LocalDateTime.now() else defaultAutoClose
                    attendanceRepo.autoCloseAttendance(open.attendanceId, autoCloseTime)
                }
            }

            // ------------------------------
            // If CHECK_IN: determine LATE vs PRESENT
            // ------------------------------
            var recorded: AttendanceRepository.AttendanceRow? = null
            if (action == "CHECK_IN") {
                // decide status: LATE if now after shiftStart
                val now = LocalDateTime.now()
                val isLate = now.toLocalTime().isAfter(shiftStart)

                // record check-in. recordCheck currently sets status to PRESENT; to mark LATE we update afterwards if needed.
                recorded = attendanceRepo.recordCheck(employeeId, "CHECK_IN", source = "QR")

                if (isLate && recorded != null) {
                    attendanceRepo.updateStatus(recorded.attendanceId, "LATE")
                    recorded = attendanceRepo.getTodayAttendance(employeeId)
                }
            } else { // CHECK_OUT
                recorded = attendanceRepo.recordCheck(employeeId, "CHECK_OUT", source = "QR")
            }

            // ------------------------------
            // Log the QR usage with userId + attendanceId (if available)
            // ------------------------------
            try {
                QRScanRepository.logScan(deviceId, window, userId = userId, attendanceId = recorded?.attendanceId)
            } catch (e: Exception) {
                println("Warning: failed to log QR scan for device=$deviceId user=$userId: ${e.message}")
            }

            return SimpleResponse(true, if (action == "CHECK_IN") "Checked in successfully" else "Checked out successfully")
        } catch (e: IllegalStateException) {
            return SimpleResponse(false, e.message ?: "Invalid attendance operation")
        } catch (e: Exception) {
            return SimpleResponse(false, "Internal error: ${e.message}")
        }
    }

    // -------------------------
    // Helpers used by routes
    // -------------------------
    fun getTodayAttendance(userId: Int): AttendanceTodayResponse {
        val employeeId = attendanceRepo.findEmployeeIdForUser(userId)
        if (employeeId == null) {
            return AttendanceTodayResponse(false, null, null, canCheckIn = true, canCheckOut = false)
        }
        val row = attendanceRepo.getTodayAttendance(employeeId)
        val checkInTime = row?.checkIn?.format(timeFormatter)
        val checkOutTime = row?.checkOut?.format(timeFormatter)
        val checkedIn = row?.checkIn != null
        val canCheckIn = !checkedIn
        val canCheckOut = checkedIn && row?.checkOut == null
        return AttendanceTodayResponse(
            checkedIn = checkedIn,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            canCheckIn = canCheckIn,
            canCheckOut = canCheckOut
        )
    }

    fun getHistory(userId: Int, limit: Int): List<AttendanceHistoryItem> {
        val employeeId = attendanceRepo.findEmployeeIdForUser(userId) ?: return emptyList()
        val rows = attendanceRepo.getHistory(employeeId, limit)
        return rows.map { r ->
            AttendanceHistoryItem(
                date = r.date.toString(),
                checkIn = r.checkIn?.format(timeFormatter),
                checkOut = r.checkOut?.format(timeFormatter),
                status = r.status ?: "UNKNOWN"
            )
        }
    }
}
