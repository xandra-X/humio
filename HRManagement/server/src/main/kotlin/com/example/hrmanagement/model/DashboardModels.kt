package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardResponse(
    val employee: EmployeeSummary,
    val todayStatus: TodayStatus,
    val leaveSummary: LeaveSummary,
    val recentActivities: List<RecentActivity>
)

@Serializable
data class EmployeeSummary(
    val fullName: String,
    val employeeCode: String,
    val jobTitle: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class TodayStatus(
    val checkedIn: Boolean,
    val checkInTime: String? = null,
    val canCheckIn: Boolean,
    val canCheckOut: Boolean
)

@Serializable
data class LeaveSummary(
    val leaveBalanceDays: Int,
    val usedThisMonthDays: Int
)

@Serializable
data class RecentActivity(
    val type: String,
    val title: String,
    val subtitle: String,
    val createdAt: String
)

@Serializable
data class CheckInOutRequest(
    val action: String,   // "CHECK_IN" or "CHECK_OUT"
    val qr: String? = null
)
/**
 * Old code was using DashboardOverview – keep it as an alias so both names work.
 */
typealias DashboardOverview = DashboardResponse
