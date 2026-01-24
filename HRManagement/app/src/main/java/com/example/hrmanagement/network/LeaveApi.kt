package com.example.hrmanagement.network
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*
import com.example.hrmanagement.data.LeaveBalanceResponse
import kotlinx.serialization.SerialName

interface LeaveApi {

    @GET("/api/leave/balance")
    suspend fun getLeaveBalance(
        @Header("Authorization") bearer: String
    ): Response<LeaveBalanceResponse>


    @GET("api/leave/history")
    suspend fun getLeaveHistory(
        @Header("Authorization") bearer: String
    ): Response<List<LeaveHistoryDto>>


    @POST("/api/leave/request")
    suspend fun submitLeave(
        @Header("Authorization") bearer: String,
        @Body req: LeaveRequestDto
    ): Response<SimpleResponse>
}

/* ---------- DTOs ---------- */
@Serializable
data class LeaveRequestDto(
    @SerialName("start_date")
    val startDate: String,

    @SerialName("end_date")
    val endDate: String,

    @SerialName("leave_type")
    val leaveType: String,

    val days: Double,
    val reason: String? = null
)


data class LeaveHistoryDto(
    val leaveType: String,
    val days: Double,
    val reason: String?,
    val status: String
)


data class SimpleResponse(
    val success: Boolean,
    val message: String
)
