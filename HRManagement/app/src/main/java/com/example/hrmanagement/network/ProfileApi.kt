package com.example.hrmanagement.network

import com.example.hrmanagement.data.profile.ProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ProfileApi {
    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") bearer: String
    ): Response<ProfileResponse>
}

