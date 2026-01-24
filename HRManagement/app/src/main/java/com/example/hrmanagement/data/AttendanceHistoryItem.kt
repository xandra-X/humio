package com.example.hrmanagement.data

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceHistoryItem(
    val date: String,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val status: String? = null
)
