package com.example.hrmanagement.data

data class VerifyResetCodeRequest(
    val email: String,
    val code: String
)
