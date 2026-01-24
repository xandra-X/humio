package com.example.hrmanagement.service

import com.example.hrmanagement.repo.DashboardRepository
import com.example.hrmanagement.model.CheckInOutRequest
import com.example.hrmanagement.model.DashboardOverview
import com.example.hrmanagement.model.SimpleResponse

class DashboardService(
    private val repo: DashboardRepository,
    private val attendanceService: AttendanceService = AttendanceService()
) {

    fun getDashboard(userId: Int): DashboardOverview = repo.getDashboardOverview(userId)

    // keep same signature used by existing DashboardRoutes (userId, request)
    fun handleCheck(userId: Int, request: CheckInOutRequest): SimpleResponse {
        // delegate to attendanceService
        return attendanceService.handleCheck(userId, request)
    }
}
