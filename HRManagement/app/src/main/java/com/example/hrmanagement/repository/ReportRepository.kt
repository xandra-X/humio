package com.example.hrmanagement.repository

import com.example.hrmanagement.network.ReportRequest
import com.example.hrmanagement.network.RetrofitClient

class ReportRepository {

    suspend fun submit(token: String, title: String, message: String): Boolean {
        val res = RetrofitClient.reportApi.submitReport(
            "Bearer $token",
            ReportRequest(title, message)
        )
        return res.isSuccessful
    }
}
