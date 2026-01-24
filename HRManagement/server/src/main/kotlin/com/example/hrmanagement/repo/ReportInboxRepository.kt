package com.example.hrmanagement.repo

import com.example.hrmanagement.db.table.ReportInboxTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object ReportInboxRepository {

    fun create(
        senderId: Int,
        senderRole: String,
        title: String,
        message: String
    ) {
        transaction {
            ReportInboxTable.insert {
                it[ReportInboxTable.senderId] = senderId
                it[ReportInboxTable.senderRole] = senderRole
                it[ReportInboxTable.title] = title
                it[ReportInboxTable.message] = message
                it[ReportInboxTable.status] = "UNREAD"
                it[ReportInboxTable.createdAt] = LocalDateTime.now()
            }
        }
    }

    fun findAll(): List<ResultRow> =
        transaction {
            ReportInboxTable
                .selectAll()
                .orderBy(ReportInboxTable.createdAt, SortOrder.DESC)
                .toList()
        }

    fun findById(id: Int): ResultRow? =
        transaction {
            ReportInboxTable
                .select { ReportInboxTable.reportId eq id }
                .singleOrNull()
        }
}
