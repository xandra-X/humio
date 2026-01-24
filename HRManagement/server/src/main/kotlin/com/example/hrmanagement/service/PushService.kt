package com.example.hrmanagement.service

import com.example.hrmanagement.repo.DeviceRepository
import com.example.hrmanagement.repo.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification


object PushService {

    fun sendToUser(userId: Int, title: String, body: String) {

        val tokens = DeviceRepository.getTokensForUser(userId)

        if (tokens.isEmpty()) return

        tokens.forEach { token ->
            try {
                val message = Message.builder()
                    .setToken(token)
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build()
                    )
                    .build()

                FirebaseMessaging.getInstance().send(message)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
