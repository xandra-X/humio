package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class SimpleResponse(
    val success: Boolean,
    val message: String
)
