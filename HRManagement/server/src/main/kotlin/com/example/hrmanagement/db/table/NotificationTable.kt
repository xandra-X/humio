package com.example.hrmanagement.db.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object NotificationTable : Table("notifications") {

    val notificationId = integer("notification_id").autoIncrement()
    val recipientUserId = integer("recipient_user_id")
    val message = text("message")
    val read = bool("read").default(false)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(notificationId)
}
