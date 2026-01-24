package com.example.hrmanagement.data

data class NotificationDto(
    val notificationId: Int,
    val message: String,
    val createdAt: String,
    val read: Boolean
)

