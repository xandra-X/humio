package com.example.hrmanagement.network

import com.example.hrmanagement.data.CheckInOutRequest
import com.example.hrmanagement.data.profile.ProfileResponse
import com.example.hrmanagement.network.models.CountRes
import com.example.hrmanagement.network.models.SimpleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    // ---------------- Attendance ----------------
    @POST("api/attendance/check")
    suspend fun check(
        @Header("Authorization") authorization: String,
        @Header("X-User-Id") userId: String? = null,
        @Body req: CheckInOutRequest
    ): Response<SimpleResponse>

    // ---------------- Notifications ----------------
    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("Authorization") token: String
    ): Response<CountRes>

    @GET("profile")
    suspend fun getProfile(): ProfileResponse
}
