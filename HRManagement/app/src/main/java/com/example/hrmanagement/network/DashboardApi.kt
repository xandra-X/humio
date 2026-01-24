package com.example.hrmanagement.network

import com.example.hrmanagement.data.AttendanceHistoryItem
import com.example.hrmanagement.data.AttendanceTodayResponse
import com.example.hrmanagement.data.CheckInOutRequest
import com.example.hrmanagement.model.DashboardResponse
import com.example.hrmanagement.data.SimpleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface DashboardApi {

    /**
     * Legacy endpoint used by DashboardActivity
     * GET /api/dashboard
     */
    @GET("/api/dashboard")
    suspend fun getDashboard(
        @Header("Authorization") bearerToken: String?,
        @Header("X-User-Id") userId: String?
    ): Response<DashboardResponse>

    /**
     * GET /api/attendance/today
     */
    @GET("/api/attendance/today")
    suspend fun getToday(
        @Header("Authorization") bearerToken: String?,
        @Header("X-User-Id") userId: String?
    ): Response<AttendanceTodayResponse>

    /**
     * GET /api/attendance/history?limit=20
     */
    @GET("/api/attendance/history")
    suspend fun getHistory(
        @Header("Authorization") bearerToken: String?,
        @Header("X-User-Id") userId: String?,
        @Query("limit") limit: Int = 20
    ): Response<List<AttendanceHistoryItem>>

    /**
     * POST /api/attendance/check
     */
    @POST("/api/attendance/check")
    suspend fun checkInOut(
        @Header("Authorization") bearerToken: String?,
        @Header("X-User-Id") userId: String?,
        @Body body: CheckInOutRequest
    ): Response<SimpleResponse>
}
