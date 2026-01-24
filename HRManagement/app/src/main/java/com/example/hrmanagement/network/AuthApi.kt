package com.example.hrmanagement.network

import com.example.hrmanagement.data.ForgotPasswordRequest
import com.example.hrmanagement.data.LoginRequest
import com.example.hrmanagement.data.LoginResponse
import com.example.hrmanagement.data.ResetPasswordRequest
import com.example.hrmanagement.data.SimpleResponse
import com.example.hrmanagement.data.VerifyResetCodeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<SimpleResponse>

    @POST("/api/auth/verify-reset-code")
    suspend fun verifyResetCode(
        @Body request: VerifyResetCodeRequest
    ): Response<SimpleResponse>

    @POST("/api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<SimpleResponse>
}
