package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val profile: ProfileInfo,
    val pay: PayInfo,
    val overtime: List<OvertimeItem>
)

@Serializable
data class ProfileInfo(
    val fullName: String,
    val employeeCode: String,
    val email: String,
    val department: String,
    val role: String,
    val shift: String,
    val avatarUrl: String?
)

@Serializable
data class PayInfo(
    val annualSalary: Double,
    val monthlySalary: Double,
    val totalOvertimePay: Double
)

@Serializable
data class OvertimeItem(
    val date: String,
    val hours: Double,
    val amount: Double,
    val status: String
)
