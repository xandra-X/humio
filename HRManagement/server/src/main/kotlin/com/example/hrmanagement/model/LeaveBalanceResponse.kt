package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaveBalanceResponse(
    val leaveBalanceDays: Int,
    val medicalLeaveDays: Int,
    val casualLeaveDays: Int,
    val unpaidLeaveDays: Int
)
