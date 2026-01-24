package com.example.hrmanagement.repo

import com.example.hrmanagement.model.*
import com.example.hrmanagement.repo.UserRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import com.example.hrmanagement.repo.LeaveRepository

/**
 * DashboardRepository - real implementation that reads from DB.
 */
class DashboardRepository {

    // Local Exposed table mappings (keep names consistent with your DB)
    private object EmployeeTable : Table("Employee") {
        val employeeId = integer("employee_id")
        val employeeCode = varchar("employee_code", 50)
        val hireDate = date("hire_date").nullable()
        val jobTitle = varchar("job_title", 150).nullable()
        val userId = integer("user_id").nullable()
        override val primaryKey = PrimaryKey(employeeId)
    }

    private object AttendanceTable : Table("Attendance") {
        val attendanceId = integer("attendance_id")
        val employeeId = integer("employee_id")
        val attDate = date("date")                          // column name is "date"
        val checkIn = datetime("check_in").nullable()
        val checkOut = datetime("check_out").nullable()
        val status = varchar("status", 20)
        val sourceCol = varchar("source", 20)               // renamed to avoid collision
        override val primaryKey = PrimaryKey(attendanceId)
    }

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    /**
     * Return dashboard overview populated from DB (User table) when possible.
     */
    fun getDashboardOverview(userId: Int): DashboardOverview {
        val today = LocalDate.now()

        // fetch user from DB (returns null if not found)
        val user = UserRepository.findById(userId)

        // determine avatar URL as before
        val profileImageFromDb = user?.profileImage
        val avatarUrl = profileImageFromDb?.let { raw ->
            when {
                raw.isBlank() -> null
                raw.startsWith("http", ignoreCase = true) -> raw
                raw.startsWith("/") -> raw
                else -> "/uploads/profile_images/$raw"
            }
        }

        val employeeSummary = if (user != null) {
            EmployeeSummary(
                fullName = user.fullName ?: user.username,
                employeeCode = user.username.ifBlank { user.id.toString() },
                jobTitle = user.userType ?: "",
                avatarUrl = avatarUrl
            )
        } else {
            EmployeeSummary(
                fullName = "Demo User",
                employeeCode = "EMP001",
                jobTitle = "UI/UX Designer",
                avatarUrl = avatarUrl
            )
        }

        // -------------------------
        // Find employee_id (if any) associated with this user
        // -------------------------
        val employeeId: Int? = transaction {
            EmployeeTable
                .slice(EmployeeTable.employeeId)
                .select { EmployeeTable.userId eq userId }
                .limit(1)
                .singleOrNull()
                ?.get(EmployeeTable.employeeId)
        }

        // -------------------------
        // Today attendance row (if present)
        // -------------------------
        val (todayStatus, todayCheckInTime) = if (employeeId != null) {
            transaction {
                val row = AttendanceTable.select {
                    (AttendanceTable.employeeId eq employeeId) and (AttendanceTable.attDate eq today)
                }.singleOrNull()

                if (row != null) {
                    val checkInDt: LocalDateTime? = row[AttendanceTable.checkIn]
                    val checkOutDt: LocalDateTime? = row[AttendanceTable.checkOut]
                    val formattedCheckIn = checkInDt?.format(timeFormatter)

                    val canCheckIn = (checkInDt == null)
                    val canCheckOut = (checkInDt != null && checkOutDt == null)

                    Pair(
                        TodayStatus(
                            checkedIn = checkInDt != null,
                            checkInTime = formattedCheckIn,
                            canCheckIn = canCheckIn,
                            canCheckOut = canCheckOut
                        ),
                        formattedCheckIn
                    )
                } else {
                    Pair(
                        TodayStatus(
                            checkedIn = false,
                            checkInTime = null,
                            canCheckIn = true,
                            canCheckOut = false
                        ),
                        null
                    )
                }
            }
        } else {
            Pair(
                TodayStatus(
                    checkedIn = false,
                    checkInTime = null,
                    canCheckIn = true,
                    canCheckOut = false
                ),
                null
            )
        }

        // -------------------------
        // Leave summary logic:
        //  - monthsEmployed = months between hire_date (if any) and now (full months)
        //  - accrued = monthsEmployed * 3
        //  - absences = count of Attendance rows with status = 'ABSENT' (all time)
        //  - leaveBalanceDays = max(0, accrued - absences)
        //  - usedThisMonthDays = dayOfMonth (per your requirement)
        // -------------------------
        val leaveBalanceDays =
            LeaveRepository
                .getLeaveBalancesForUser(userId)
                .sumOf { it.balanceDays }

        val usedThisMonthDays = LocalDate.now().dayOfMonth

        val leaveSummary = LeaveSummary(
            leaveBalanceDays = leaveBalanceDays,
            usedThisMonthDays = usedThisMonthDays
        )

        return DashboardOverview(
            employee = employeeSummary,
            todayStatus = todayStatus,
            leaveSummary = leaveSummary,
            recentActivities = emptyList()
        )
    }
}
