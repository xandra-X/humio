package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class VerifyResetCodeRequest(
    val email: String,
    val code: String
)
