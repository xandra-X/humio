package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaveRequestDto(
    val startDate: String,
    val endDate: String,
    val leaveType: String,
    val days: Double,
    val reason: String? = null
)