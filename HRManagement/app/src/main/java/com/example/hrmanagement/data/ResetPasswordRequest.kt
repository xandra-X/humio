package com.example.hrmanagement.data

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)
