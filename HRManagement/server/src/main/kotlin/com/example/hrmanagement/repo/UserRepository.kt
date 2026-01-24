package com.example.hrmanagement.repo

import com.example.hrmanagement.model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

// Table mapping for your `User` table
object UserTable : Table("User") {
    val id = integer("user_id").autoIncrement()
    val username = varchar("username", 255)
    val password = varchar("password_hash", 255)
    val email = varchar("email", 255)
    val fullName = varchar("full_name", 255).nullable()
    val profileImage = varchar("profile_image", 512).nullable()
    val userType = varchar("user_type", 100).nullable()

    override val primaryKey = PrimaryKey(id)
}

object UserRepository {

    fun findById(userId: Int): User? = transaction {
        UserTable
            .select { UserTable.id eq userId }
            .singleOrNull()
            ?.let { row ->
                User(
                    id = row[UserTable.id],
                    username = row[UserTable.username],
                    email = row[UserTable.email],
                    passwordHash = row[UserTable.password],
                    fullName = row[UserTable.fullName],
                    profileImage = row[UserTable.profileImage],
                    userType = row[UserTable.userType]
                )
            }
    }

    fun findByEmail(email: String): User? = transaction {
        UserTable
            .select { UserTable.email eq email }
            .singleOrNull()
            ?.let { row ->
                User(
                    id = row[UserTable.id],
                    username = row[UserTable.username],
                    email = row[UserTable.email],
                    passwordHash = row[UserTable.password],
                    fullName = row[UserTable.fullName],
                    profileImage = row[UserTable.profileImage],
                    userType = row[UserTable.userType]
                )
            }
    }

    fun updatePassword(userId: Int, newHash: String): Boolean = transaction {
        val rows = UserTable.update({ UserTable.id eq userId }) {
            it[password] = newHash
        }
        rows > 0
    }
}
