package com.example.hrmanagement.repo

import com.example.hrmanagement.db.table.NotificationTable
import com.example.hrmanagement.model.NotificationDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object NotificationRepository {

    fun create(userId: Int, message: String) = transaction {
        println("🔥 NotificationRepository.create() CALLED")
        println("🔥 userId = $userId")
        println("🔥 message = $message")

        NotificationTable.insert {
            it[recipientUserId] = userId
            it[NotificationTable.message] = message
            it[read] = false
        }
    }

    fun getUnreadCount(userId: Int): Int = transaction {
        NotificationTable
            .select {
                (NotificationTable.recipientUserId eq userId) and
                        (NotificationTable.read eq false)
            }
            .count()
            .toInt()
    }

    fun getAll(userId: Int): List<NotificationDto> = transaction {
        NotificationTable
            .select { NotificationTable.recipientUserId eq userId }
            .orderBy(NotificationTable.createdAt, SortOrder.DESC)
            .map {
                NotificationDto(
                    id = it[NotificationTable.notificationId],
                    message = it[NotificationTable.message],
                    read = it[NotificationTable.read],
                    createdAt = it[NotificationTable.createdAt].toString()
                )
            }
    }

    fun markAsRead(userId: Int) = transaction {
        NotificationTable.update(
            {
                (NotificationTable.recipientUserId eq userId) and
                        (NotificationTable.read eq false)
            }
        ) {
            it[read] = true
        }
    }
}
