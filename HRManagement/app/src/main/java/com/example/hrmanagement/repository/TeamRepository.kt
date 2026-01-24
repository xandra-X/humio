package com.example.hrmanagement.repository

import android.content.Context
import com.example.hrmanagement.data.team.TeamResponse
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.util.SessionManager

class TeamRepository(context: Context) {

    private val api = RetrofitClient.teamApi
    private val sessionManager = SessionManager(context)

    suspend fun getMyDepartmentTeam(): TeamResponse {
        val token = sessionManager.fetchAuthToken()
            ?: throw IllegalStateException("No token found")

        return api.getMyDepartmentTeam("Bearer $token")
    }
}
