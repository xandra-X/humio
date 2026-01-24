package com.example.hrmanagement.service

import com.example.hrmanagement.model.*
import com.example.hrmanagement.repo.LeaveRepository
import java.time.LocalDate

class LeaveService {

    fun getLeaveBalance(userId: Int): LeaveBalanceResponse {
        val balances = LeaveRepository.getLeaveBalancesForUser(userId)

        fun get(type: String): Int =
            balances.firstOrNull { it.type.equals(type, ignoreCase = true) }
                ?.balanceDays ?: 0

        return LeaveBalanceResponse(
            leaveBalanceDays = get("ANNUAL"),
            medicalLeaveDays = get("MEDICAL"),
            casualLeaveDays = get("CASUAL"),
            unpaidLeaveDays = get("UNPAID")

        )
    }

    fun submitLeave(userId: Int, req: LeaveRequestDto): SimpleResponse {

        val employeeId = LeaveRepository.findEmployeeIdForUser(userId)
            ?: return SimpleResponse(false, "Employee not found")

        // ✅ Check balance for selected leave type
        val balances = LeaveRepository.getLeaveBalancesForUser(userId)
        val selectedType = normalizeLeaveType(req.leaveType)

        val selectedBalance = balances
            .firstOrNull { it.type == selectedType }
            ?.balanceDays ?: 0

        if (selectedBalance <= 0) {
            return SimpleResponse(
                false,
                "You have no $selectedType leave balance left."
            )
        }

        val start = LocalDate.parse(req.startDate)
        val end = LocalDate.parse(req.endDate)

        if (start.isAfter(end)) {
            return SimpleResponse(false, "Invalid date range")
        }

        val ok = LeaveRepository.createLeaveRequest(
            employeeId = employeeId,
            leaveType = selectedType,
            startDate = start,
            endDate = end,
            days = req.days,
            reason = req.reason ?: ""
        )

        return if (ok)
            SimpleResponse(true, "Leave request submitted")
        else
            SimpleResponse(false, "Monthly leave limit exceeded")
    }

    fun getLeaveHistory(userId: Int): List<LeaveHistoryDto> {

        val employeeId = LeaveRepository.findEmployeeIdForUser(userId)
            ?: return emptyList()

        return LeaveRepository
            .getHistoryForUser(userId)
            .map {
                LeaveHistoryDto(
                    type = it.leaveType,
                    daysText = "${it.days} days",
                    note = it.reason ?: "",
                    status = it.status
                )
            }
    }

    // ✅ CLASS-LEVEL helper (correct place)
    private fun normalizeLeaveType(type: String): String {
        return type.uppercase()
    }

}
