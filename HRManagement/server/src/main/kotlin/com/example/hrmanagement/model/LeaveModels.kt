package com.example.hrmanagement.model
import kotlinx.serialization.Serializable

@Serializable
data class LeaveHistoryDto(
    val type: String,
    val daysText: String,
    val note: String,
    val status: String
)
