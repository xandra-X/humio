package com.example.hrmanagement.repo

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import com.example.hrmanagement.model.AttendanceStatus
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import com.example.hrmanagement.db.table.AttendanceTable
import com.example.hrmanagement.db.table.EmployeeTable

/**
 * Single canonical AttendanceRepository used by services/tasks/routes.
 */
class AttendanceRepository {


    data class AttendanceRow(
        val attendanceId: Int,
        val employeeId: Int,
        val date: LocalDate,
        val checkIn: LocalDateTime?,
        val checkOut: LocalDateTime?,
        val status: String?,
        val source: String?
    )

    /**
     * Get today's attendance for an employee (or null).
     */
    fun getTodayAttendance(employeeId: Int): AttendanceRow? {
        val today = LocalDate.now()
        return transaction {
            AttendanceTable.select {
                (AttendanceTable.employeeId eq employeeId) and (AttendanceTable.date eq today)
            }.singleOrNull()?.let { rowToAttendance(it) }
        }
    }
    fun findEmployeesWithoutAttendance(date: LocalDate): List<Int> = transaction {
        val attendedEmployeeIds =
            AttendanceTable
                .slice(AttendanceTable.employeeId)
                .select { AttendanceTable.date eq date }
                .map { it[AttendanceTable.employeeId] }
                .toSet()

        EmployeeTable
            .slice(EmployeeTable.employeeId)
            .selectAll()
            .map { it[EmployeeTable.employeeId] }
            .filterNot { it in attendedEmployeeIds }
    }
    fun markAutoCheckedOut(attendanceId: Int) = transaction {
        AttendanceTable.update({ AttendanceTable.attendanceId eq attendanceId }) {
            it[autoCheckedOut] = true
            it[updatedAt] = LocalDateTime.now()
        }
    }

    fun markAbsent(employeeId: Int, date: LocalDate) = transaction {
        AttendanceTable.insert {
            it[AttendanceTable.employeeId] = employeeId
            it[AttendanceTable.date] = date
            it[AttendanceTable.checkIn] = null
            it[AttendanceTable.checkOut] = null
            it[AttendanceTable.status] = "ABSENT"
            it[AttendanceTable.sourceCol] = "MANUAL"
            it[AttendanceTable.updatedAt] = LocalDateTime.now()
            it[AttendanceTable.autoCheckedOut] = false
        }
    }


    fun findAllOpenAttendancesForDate(date: LocalDate): List<AttendanceRow> = transaction {
        AttendanceTable.select {
            (AttendanceTable.date eq date) and
                    (AttendanceTable.checkIn.isNotNull()) and
                    (AttendanceTable.checkOut.isNull())
        }.map { rowToAttendance(it) }
    }


    /**
     * Find employee_id for a given user_id (or null).
     */
    fun findEmployeeIdForUser(userId: Int): Int? = transaction {
        EmployeeTable.select { EmployeeTable.userId eq userId }
            .limit(1)
            .mapNotNull { it[EmployeeTable.employeeId] }
            .singleOrNull()
    }

    /**
     * Find any open (checked-in, not checked-out) attendance for the given employee.
     */
    fun findOpenAttendanceForEmployee(employeeId: Int): AttendanceRow? = transaction {
        AttendanceTable.select {
            (AttendanceTable.employeeId eq employeeId) and
                    (AttendanceTable.checkIn.isNotNull()) and
                    (AttendanceTable.checkOut.isNull())
        }.orderBy(AttendanceTable.date, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { rowToAttendance(it) }
    }

    /**
     * Find all open attendances whose date is <= cutoffDate.
     * Used by auto-checkout task to close old opens.
     */
    fun findAllOpenAttendancesBefore(cutoffDate: LocalDate): List<AttendanceRow> = transaction {
        AttendanceTable.select {
            (AttendanceTable.checkIn.isNotNull()) and
                    (AttendanceTable.checkOut.isNull()) and
                    (AttendanceTable.date lessEq cutoffDate)
        }.map { rowToAttendance(it) }
    }

    /**
     * Auto-close an attendance by setting check_out to specified time.
     */
    fun autoCloseAttendance(attendanceId: Int, closeTime: LocalDateTime) = transaction {
        AttendanceTable.update({ AttendanceTable.attendanceId eq attendanceId }) {
            it[checkOut] = closeTime
        }
    }

    /**
     * Update status text (e.g. set to "LATE")
     */
    fun updateStatus(attendanceId: Int, newStatus: String) = transaction {
        AttendanceTable.update({ AttendanceTable.attendanceId eq attendanceId }) {
            it[status] = newStatus
        }
    }

    /**
     * Record a check action (CHECK_IN or CHECK_OUT). Throws IllegalStateException on invalid sequence.
     */
    fun recordCheck(employeeId: Int, action: String, source: String = "QR"): AttendanceRow {
        val today = LocalDate.now()
        return transaction {
            val existing = AttendanceTable.select {
                (AttendanceTable.employeeId eq employeeId) and (AttendanceTable.date eq today)
            }.singleOrNull()

            val now = LocalDateTime.now()

            if (action == "CHECK_IN") {
                if (existing != null && existing[AttendanceTable.checkIn] != null) {
                    throw IllegalStateException("Already checked in")
                }

                if (existing == null) {
                    val inserted = AttendanceTable.insert { itRow ->
                        itRow[AttendanceTable.employeeId] = employeeId
                        itRow[AttendanceTable.date] = today
                        itRow[AttendanceTable.checkIn] = now
                        itRow[AttendanceTable.checkOut] = null
                        itRow[AttendanceTable.status] = "PRESENT"
                        itRow[AttendanceTable.sourceCol] = source
                    }
                    val id = inserted[AttendanceTable.attendanceId]
                    AttendanceTable.select { AttendanceTable.attendanceId eq id }.single().let(::rowToAttendance)
                } else {
                    AttendanceTable.update({ AttendanceTable.attendanceId eq existing[AttendanceTable.attendanceId] }) { itRow ->
                        itRow[AttendanceTable.checkIn] = now
                        itRow[AttendanceTable.status] = "PRESENT"
                        itRow[AttendanceTable.sourceCol] = source
                    }
                    AttendanceTable.select { AttendanceTable.attendanceId eq existing[AttendanceTable.attendanceId] }.single().let(::rowToAttendance)
                }
            } else if (action == "CHECK_OUT") {
                if (existing == null || existing[AttendanceTable.checkIn] == null) {
                    throw IllegalStateException("Cannot check out without check-in")
                }
                if (existing[AttendanceTable.checkOut] != null) {
                    throw IllegalStateException("Already checked out")
                }

                AttendanceTable.update({ AttendanceTable.attendanceId eq existing[AttendanceTable.attendanceId] }) { itRow ->
                    itRow[AttendanceTable.checkOut] = now
                }
                AttendanceTable.select { AttendanceTable.attendanceId eq existing[AttendanceTable.attendanceId] }.single().let(::rowToAttendance)
            } else {
                throw IllegalArgumentException("Unknown action: $action")
            }
        }
    }

    /**
     * Attendance history pager.
     */
    fun getHistory(employeeId: Int, limit: Int = 20): List<AttendanceRow> {
        return transaction {
            AttendanceTable.select { AttendanceTable.employeeId eq employeeId }
                .orderBy(AttendanceTable.date, SortOrder.DESC)
                .limit(limit)
                .map { rowToAttendance(it) }
        }
    }

    private fun rowToAttendance(row: ResultRow): AttendanceRow {
        return AttendanceRow(
            attendanceId = row[AttendanceTable.attendanceId],
            employeeId = row[AttendanceTable.employeeId],
            date = row[AttendanceTable.date],
            checkIn = row[AttendanceTable.checkIn],
            checkOut = row[AttendanceTable.checkOut],
            status = row[AttendanceTable.status],
            source = row[AttendanceTable.sourceCol]
        )
    }
    fun markOnLeave(employeeId: Int, date: LocalDate) = transaction {

        val row =
            AttendanceTable
                .select {
                    (AttendanceTable.employeeId eq employeeId) and
                            (AttendanceTable.date eq date)
                }
                .singleOrNull()

        if (row == null) {

            // Case 1: No attendance exists → insert
            AttendanceTable.insert {
                it[AttendanceTable.employeeId] = employeeId
                it[AttendanceTable.date] = date
                it[AttendanceTable.checkIn] = null
                it[AttendanceTable.checkOut] = null
                it[AttendanceTable.status] = "ON_LEAVE"
                it[AttendanceTable.sourceCol] = "MANUAL"
                it[AttendanceTable.updatedAt] = LocalDateTime.now()
                it[AttendanceTable.autoCheckedOut] = false
            }

        } else {

            // Case 2: Attendance exists → FORCE override
            AttendanceTable.update(
                {
                    (AttendanceTable.employeeId eq employeeId) and
                            (AttendanceTable.date eq date)
                }
            ) {
                it[checkIn] = null
                it[checkOut] = null
                it[status] = "ON_LEAVE"
                it[sourceCol] = "MANUAL"
                it[updatedAt] = LocalDateTime.now()
                it[autoCheckedOut] = false
            }
        }
    }
    fun hasAttendance(employeeId: Int, date: LocalDate): Boolean =
        transaction {
            AttendanceTable
                .select {
                    (AttendanceTable.employeeId eq employeeId) and
                            (AttendanceTable.date eq date)
                }
                .count() > 0
        }


}
