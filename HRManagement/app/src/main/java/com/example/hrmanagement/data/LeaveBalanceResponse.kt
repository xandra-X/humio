package com.example.hrmanagement.data

data class LeaveBalanceResponse(
    val leaveBalanceDays: Int,
    val medicalLeaveDays: Int,
    val casualLeaveDays: Int,
    val unpaidLeaveDays: Int
)