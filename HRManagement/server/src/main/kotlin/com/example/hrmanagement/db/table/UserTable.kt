package com.example.hrmanagement.db.table

import org.jetbrains.exposed.sql.Table

object UserTable : Table("User") {

    val userId = integer("user_id").autoIncrement()
    val fullName = varchar("full_name", 255).nullable()
    val email = varchar("email", 255)
    val profileImage = varchar("profile_image", 255).nullable()

    override val primaryKey = PrimaryKey(userId)
}
