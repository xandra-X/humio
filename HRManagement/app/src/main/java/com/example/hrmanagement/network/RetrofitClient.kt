package com.example.hrmanagement.network

import com.example.hrmanagement.data.LeaveBalanceResponse
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.example.hrmanagement.data.NotificationDto
import com.example.hrmanagement.data.SimpleResponse
import com.example.hrmanagement.data.profile.ProfileResponse

object RetrofitClient {

    const val BASE_URL = "http://10.0.2.2:8080/"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ================= APIs =================

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val dashboardApi: DashboardApi = retrofit.create(DashboardApi::class.java)
    val leaveApi: LeaveApi = retrofit.create(LeaveApi::class.java)
    val notificationApi: NotificationApi = retrofit.create(NotificationApi::class.java)
    val deviceApi: DeviceApi = retrofit.create(DeviceApi::class.java)

    val profileApi: ProfileApi = retrofit.create(ProfileApi::class.java)
    val teamApi: TeamApi by lazy {
        retrofit.create(TeamApi::class.java)
    }
    val reportApi: ReportApi = retrofit.create(ReportApi::class.java)



    // ================= DEVICE (FCM) =================

    interface DeviceApi {

        @POST("api/device/register")
        suspend fun registerDevice(
            @Header("Authorization") bearer: String,
            @Body body: DeviceRegisterRequest
        ): Response<Unit>
    }

    data class DeviceRegisterRequest(
        val deviceUuid: String,
        val fcmToken: String
    )

    // ================= LEAVE =================

    interface LeaveApi {

        @GET("api/leave/balance")
        suspend fun getLeaveBalance(
            @Header("Authorization") bearer: String
        ): Response<LeaveBalanceResponse>

        @GET("api/leave/history")
        suspend fun getLeaveHistory(
            @Header("Authorization") bearer: String
        ): Response<List<LeaveHistoryDto>>

        @POST("api/leave/request")
        suspend fun submitLeave(
            @Header("Authorization") bearer: String,
            @Body body: LeaveRequestBody
        ): Response<SimpleResponse>
    }

    data class LeaveHistoryDto(
        @SerializedName("type")
        val leaveType: String,
        val daysText: String,
        val note: String?,
        val status: String
    )

    data class LeaveRequestBody(
        val startDate: String,
        val endDate: String,
        val leaveType: String,
        val days: Double,
        val reason: String?
    )

    interface ProfileApi {

        @GET("api/profile")
        suspend fun getProfile(
            @Header("Authorization") bearer: String
        ): Response<ProfileResponse>

    }

    // ================= NOTIFICATION =================

    interface NotificationApi {

        @GET("api/notifications")
        suspend fun getNotifications(
            @Header("Authorization") bearer: String,
            @Header("X-User-Id") userId: String
        ): Response<List<NotificationDto>>

        @POST("api/notifications/read")
        suspend fun markAsRead(
            @Header("Authorization") bearer: String,
            @Header("X-User-Id") userId: String
        ): Response<Unit>

        @GET("api/notifications/unread-count")
        suspend fun getUnreadCount(
            @Header("Authorization") bearer: String,
            @Header("X-User-Id") userId: String
        ): Response<UnreadCountResponse>
    }

    data class UnreadCountResponse(
        val count: Int)
}

