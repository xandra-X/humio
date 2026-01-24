package com.example.hrmanagement.db.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ReportInboxTable : Table("ReportInbox") {

    val reportId = integer("report_id").autoIncrement()
    val senderId = integer("sender_id")
    val senderRole = varchar("sender_role", 20)
    val title = varchar("title", 255)
    val message = text("message")
    val status = varchar("status", 20)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(reportId)
}
