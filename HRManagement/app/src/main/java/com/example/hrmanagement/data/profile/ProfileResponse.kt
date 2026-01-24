package com.example.hrmanagement.data.profile

data class ProfileResponse(
    val profile: ProfileInfo,
    val pay: PayInfo,
    val overtime: List<OvertimeItem>
)

data class ProfileInfo(
    val fullName: String,
    val employeeCode: String,
    val email: String,
    val department: String,
    val role: String,
    val shift: String,
    val avatarUrl: String?
)

@kotlinx.serialization.Serializable
data class PayInfo(
    val annualSalary: Double,
    val monthlySalary: Double,
    val totalOvertimePay: Double
)

data class OvertimeItem(
    val date: String,
    val hours: Double,
    val amount: Double,
    val status: String
)
