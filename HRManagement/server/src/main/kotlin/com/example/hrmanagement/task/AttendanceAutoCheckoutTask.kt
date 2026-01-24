package com.example.hrmanagement.task

import com.example.hrmanagement.repo.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Periodically auto-closes old open attendances.
 *
 * You can start this task from Application start (e.g. Application.kt) if desired.
 */
class AttendanceAutoCheckoutTask(
    private val attendanceRepository: AttendanceRepository = AttendanceRepository()
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        scope.launch {
            while (true) {
                try {
                    runOnce()
                } catch (t: Throwable) {
                    println("AutoCheckoutTask error: ${t.message}")
                }
                // sleep for 1 hour (adjust if needed)
                delay(60L * 60L * 1000L)
            }
        }
    }

    private fun runOnce() {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val currentTime = now.toLocalTime()

        val autoCheckoutTime = LocalTime.of(16, 30)
        val checkInStart = LocalTime.of(9, 0)
        val checkInEnd = LocalTime.of(9, 30)

        // 🔹 1. AUTO CHECK-OUT (today)
        if (currentTime >= autoCheckoutTime) {
            val openToday = attendanceRepository.findAllOpenAttendancesForDate(today)

            openToday.forEach { att ->
                val checkInTime = att.checkIn?.toLocalTime() ?: return@forEach

                if (checkInTime in checkInStart..checkInEnd) {
                    val checkoutAt = LocalDateTime.of(today, autoCheckoutTime)
                    attendanceRepository.autoCloseAttendance(att.attendanceId, checkoutAt)
                    attendanceRepository.markAutoCheckedOut(att.attendanceId)

                    println("Auto checkout attendanceId=${att.attendanceId}")
                }
            }
        }

        // 🔹 2. AUTO ABSENT
        if (currentTime >= autoCheckoutTime) {
            val employeesWithoutAttendance =
                attendanceRepository.findEmployeesWithoutAttendance(today)

            employeesWithoutAttendance.forEach { employeeId ->
                attendanceRepository.markAbsent(employeeId, today)
                println("Auto absent employeeId=$employeeId")
            }
        }

        // 🔹 3. CLOSE OLD OPEN ATTENDANCES (your existing logic)
        val cutoff = today.minusDays(1)
        val oldOpen = attendanceRepository.findAllOpenAttendancesBefore(cutoff)

        oldOpen.forEach { open ->
            val closeTime =
                open.checkIn?.plusHours(8)
                    ?: LocalDateTime.of(open.date, LocalTime.of(17, 0))

            attendanceRepository.autoCloseAttendance(open.attendanceId, closeTime)
        }
    }

}
