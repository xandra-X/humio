package com.example.hrmanagement.model

data class DashboardResponse(
    val employee: EmployeeSummary,
    val todayStatus: TodayStatus,
    val leaveSummary: LeaveSummary,
    val recentActivities: List<RecentActivity> = emptyList()
)
data class RecentActivity(
    val type: String,
    val title: String,
    val subtitle: String,
    val createdAt: String
)
data class EmployeeSummary(
    val fullName: String,
    val employeeCode: String,
    val jobTitle: String?,
    val avatarUrl: String?
)

data class TodayStatus(
    val checkedIn: Boolean,
    val checkInTime: String?,
    val canCheckIn: Boolean,
    val canCheckOut: Boolean
)

data class LeaveSummary(
    val leaveBalanceDays: Int,
    val usedThisMonthDays: Int
)



