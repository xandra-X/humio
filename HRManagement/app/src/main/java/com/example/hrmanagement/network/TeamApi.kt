package com.example.hrmanagement.network

import com.example.hrmanagement.data.team.TeamResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface TeamApi {

    @GET("/api/team/my-department")
    suspend fun getMyDepartmentTeam(
        @Header("Authorization") token: String
    ): TeamResponse
}
