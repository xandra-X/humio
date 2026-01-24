package com.example.hrmanagement.util

import com.typesafe.config.ConfigFactory
object JwtConfig {
    lateinit var secret: String
    lateinit var issuer: String
    lateinit var audience: String
    lateinit var realm: String

    fun init() {
        val config = ConfigFactory.load().getConfig("ktor.jwt")

        secret = config.getString("secret")
        issuer = config.getString("issuer")
        audience = config.getString("audience")
        realm = config.getString("realm")
    }
}
