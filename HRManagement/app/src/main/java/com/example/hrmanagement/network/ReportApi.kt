package com.example.hrmanagement.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

data class ReportRequest(
    val title: String,
    val message: String
)

interface ReportApi {

    @POST("api/reports/inbox")
    @Headers("Content-Type: application/json")
    suspend fun submitReport(
        @Header("Authorization") bearer: String,
        @Body body: ReportRequest
    ): Response<Map<String, Boolean>>
}
