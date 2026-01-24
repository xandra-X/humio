package com.example.hrmanagement.network.models

data class CountRes(
    val count: Int
)

data class SimpleResponse(
    val success: Boolean,
    val message: String?
)
