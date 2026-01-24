package com.example.hrmanagement.repo

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

// Data class used by AuthService
data class PasswordResetToken(
    val id: Int,
    val userId: Int,
    val otpCode: String,
    val expiresAt: LocalDateTime,
    val used: Boolean
)

// Exposed table mapping to MySQL table `password_reset_tokens`
object PasswordResetTable : Table("password_reset_tokens") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val otpCode = varchar("otp_code", 6)
    val expiresAt = datetime("expires_at")
    val used = bool("used")
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

// Repository implemented with Exposed DSL
object PasswordResetRepository {

    fun createToken(userId: Int, code: String, expiresAt: LocalDateTime) = transaction {
        PasswordResetTable.insert {
            it[PasswordResetTable.userId] = userId
            it[otpCode] = code
            it[PasswordResetTable.expiresAt] = expiresAt
            it[used] = false
            it[createdAt] = LocalDateTime.now()
        }
    }

    fun findValidToken(userId: Int, code: String): PasswordResetToken? = transaction {
        // 1. Fetch the token (ignoring expiry in SQL to avoid timezone mismatch issues)
        val row = PasswordResetTable
            .select {
                (PasswordResetTable.userId eq userId) and
                        (PasswordResetTable.otpCode eq code) and
                        (PasswordResetTable.used eq false)
            }
            .orderBy(PasswordResetTable.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull() ?: return@transaction null

        // 2. Check expiration in Application logic (JVM time vs JVM time)
        val expires = row[PasswordResetTable.expiresAt]
        if (expires.isBefore(LocalDateTime.now())) {
            return@transaction null
        }

        // 3. Return mapped object
        PasswordResetToken(
            id = row[PasswordResetTable.id],
            userId = row[PasswordResetTable.userId],
            otpCode = row[PasswordResetTable.otpCode],
            expiresAt = expires,
            used = row[PasswordResetTable.used]
        )
    }

    fun markUsed(id: Int) = transaction {
        PasswordResetTable.update({ PasswordResetTable.id eq id }) {
            it[used] = true
        }
    }
}
