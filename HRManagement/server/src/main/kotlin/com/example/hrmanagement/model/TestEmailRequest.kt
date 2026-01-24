package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class TestEmailRequest(
    val email: String
)
