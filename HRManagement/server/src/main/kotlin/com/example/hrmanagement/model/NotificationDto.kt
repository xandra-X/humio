package com.example.hrmanagement.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: Int,
    val message: String,
    val read: Boolean,
    val createdAt: String
)
