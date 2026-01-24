package com.example.hrmanagement.repo

import com.example.hrmanagement.db.table.EmployeeTable
import com.example.hrmanagement.db.table.LeaveRequestTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import com.example.hrmanagement.db.table.AttendanceTable
import com.example.hrmanagement.db.table.NotificationTable
import com.example.hrmanagement.service.PushService

object LeaveRepository {

    // ---------------- DATA CLASSES ----------------

    data class BalanceRow(
        val type: String,
        val balanceDays: Int,
        val description: String?
    )

    data class LeaveHistoryRow(
        val leaveType: String,
        val startDate: LocalDate,
        val days: Double,
        val reason: String?,
        val status: String
    )

    // ---------------- BASIC HELPERS ----------------

    fun findEmployeeIdForUser(userId: Int): Int? = transaction {
        (EmployeeTable)
            .select { EmployeeTable.userId eq userId }
            .map { it[EmployeeTable.employeeId] }
            .singleOrNull()
    }

    // ---------------- LEAVE BALANCE ----------------

    fun getLeaveBalancesForUser(userId: Int): List<BalanceRow> = transaction {
        val employeeId = findEmployeeIdForUser(userId) ?: return@transaction emptyList()

        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

        // Monthly quota per leave type
        val monthlyQuota = 3

        // Sum used days per leave type for current month
        val usedByType: Map<String, Double> =
            LeaveRequestTable
                .slice(LeaveRequestTable.leaveType, LeaveRequestTable.days.sum())
                .select {
                    (LeaveRequestTable.employeeId eq employeeId) and
                            (LeaveRequestTable.startDate greaterEq startOfMonth) and
                            (LeaveRequestTable.startDate lessEq endOfMonth) and
                            (LeaveRequestTable.status inList listOf("PENDING", "APPROVED"))
                }
                .groupBy(LeaveRequestTable.leaveType)
                .associate {
                    it[LeaveRequestTable.leaveType] to
                            (it[LeaveRequestTable.days.sum()]?.toDouble() ?: 0.0)
                }
        fun hasAnyLeaveBalance(userId: Int): Boolean {
            val balances = getLeaveBalancesForUser(userId)
            return balances.any { it.balanceDays > 0 }
        }


        fun remaining(type: String): Int =
            (monthlyQuota - (usedByType[type] ?: 0.0)).coerceAtLeast(0.0).toInt()

        listOf(
            BalanceRow(
                type = "ANNUAL",
                balanceDays = remaining("ANNUAL"),
                description = "Paid time off for vacation or personal plans."
            ),
            BalanceRow(
                type = "MEDICAL",
                balanceDays = remaining("MEDICAL"),
                description = "Time off for health reasons."
            ),
            BalanceRow(
                type = "CASUAL",
                balanceDays = remaining("CASUAL"),
                description = "Short notice personal leave."
            ),
            BalanceRow(
                type = "UNPAID",
                balanceDays = remaining("UNPAID"),
                description = "Leave without pay."
            )
        )
    }


    // ---------------- HISTORY ----------------

    fun getHistoryForUser(userId: Int): List<LeaveHistoryRow> = transaction {

        val employeeId = findEmployeeIdForUser(userId)
            ?: return@transaction emptyList()

        LeaveRequestTable
            .select { LeaveRequestTable.employeeId eq employeeId }
            .orderBy(LeaveRequestTable.leaveId, SortOrder.DESC)
            .map {
                LeaveHistoryRow(
                    leaveType = it[LeaveRequestTable.leaveType],
                    startDate = it[LeaveRequestTable.startDate],
                    days = it[LeaveRequestTable.days].toDouble(),
                    reason = it[LeaveRequestTable.reason],
                    status = it[LeaveRequestTable.status]
                )
            }
    }

    // ---------------- CREATE ----------------

    fun createLeaveRequest(
        employeeId: Int,
        leaveType: String,
        startDate: LocalDate,
        endDate: LocalDate,
        days: Double,
        reason: String
    ): Boolean = transaction {

        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

        val monthlyQuota = 10

        // Calculate already used days this month
        val usedDays =
            LeaveRequestTable
                .slice(LeaveRequestTable.days.sum())
                .select {
                    (LeaveRequestTable.employeeId eq employeeId) and
                            (LeaveRequestTable.leaveType eq leaveType) and
                            (LeaveRequestTable.startDate greaterEq startOfMonth) and
                            (LeaveRequestTable.startDate lessEq endOfMonth) and
                            (LeaveRequestTable.status inList listOf("PENDING", "APPROVED"))
                }
                .firstOrNull()
                ?.get(LeaveRequestTable.days.sum())
                ?.toDouble() ?: 0.0

        // ❌ Block if limit exceeded
        if (usedDays + days > monthlyQuota) {
            return@transaction false
        }

        // ✅ Insert leave
        LeaveRequestTable.insert {
            it[this.employeeId] = employeeId
            it[this.leaveType] = leaveType
            it[this.startDate] = startDate
            it[this.endDate] = endDate
            it[this.days] = days.toBigDecimal()
            it[this.reason] = reason
            it[this.status] = "PENDING"
        }

        true
    }


    // ---------------- UPDATE STATUS (HR) ----------------

    fun updateLeaveStatus(leaveId: Int, status: String): Boolean = transaction {
        LeaveRequestTable.update({ LeaveRequestTable.leaveId eq leaveId }) {
            it[LeaveRequestTable.status] = status
        } > 0
    }

    // ---------------- FIND USER FOR NOTIFICATION ----------------

    fun findUserIdByLeaveId(leaveId: Int): Int? = transaction {
        (LeaveRequestTable innerJoin EmployeeTable)
            .slice(EmployeeTable.userId)
            .select { LeaveRequestTable.leaveId eq leaveId }
            .mapNotNull { it[EmployeeTable.userId] }
            .singleOrNull()
    }
    fun findEmployeeIdByLeaveId(leaveId: Int): Int? = transaction {
        LeaveRequestTable
            .slice(LeaveRequestTable.employeeId)
            .select { LeaveRequestTable.leaveId eq leaveId }
            .map { it[LeaveRequestTable.employeeId] }
            .singleOrNull()
    }

    fun getLeaveDateRange(leaveId: Int): List<LocalDate> = transaction {
        val row = LeaveRequestTable
            .select { LeaveRequestTable.leaveId eq leaveId }
            .singleOrNull() ?: return@transaction emptyList()

        val start = row[LeaveRequestTable.startDate]
        val end = row[LeaveRequestTable.endDate]

        val dates = mutableListOf<LocalDate>()
        var current = start

        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }

        dates
    }
    fun approveLeave(leaveId: Int): Boolean {

        // 1️⃣ Get userId FIRST (outside transaction)
        val userId = findUserIdByLeaveId(leaveId) ?: return false

        // 2️⃣ DB transaction ONLY does DB work
        transaction {

            LeaveRequestTable.update(
                { LeaveRequestTable.leaveId eq leaveId }
            ) {
                it[status] = "APPROVED"
            }

            NotificationTable.insert {
                it[recipientUserId] = userId
                it[message] = "Your leave request has been approved"
                it[read] = false
            }
        }

        // 3️⃣ PUSH AFTER DB COMMIT
        PushService.sendToUser(
            userId = userId,
            title = "Leave Approved",
            body = "Your leave request has been approved"
        )

        return true
    }

    fun rejectLeave(leaveId: Int): Boolean = transaction {

        LeaveRequestTable.update({ LeaveRequestTable.leaveId eq leaveId }) {
            it[status] = "REJECTED"
        }

        val userId = findUserIdByLeaveId(leaveId)
        if (userId != null) {
            PushService.sendToUser(
                userId = userId,
                title = "Leave Rejected",
                body = "Your leave request has been rejected"
            )
        }

        true
    }





}
