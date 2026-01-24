package com.example.hrmanagement.data

data class SimpleResponse(
    val success: Boolean,
    val message: String?,
    val token: String? = null   // optional token for login
)
