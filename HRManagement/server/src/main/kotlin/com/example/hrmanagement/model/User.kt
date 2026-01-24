package com.example.hrmanagement.model

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val passwordHash: String,
    val fullName: String?,
    val profileImage: String?,
    val userType: String?
)
