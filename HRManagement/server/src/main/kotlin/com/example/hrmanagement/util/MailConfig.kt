package com.example.hrmanagement.util

import com.typesafe.config.ConfigFactory

object MailConfig {
    lateinit var host: String
    var port: Int = 587
    lateinit var username: String
    lateinit var password: String
    lateinit var fromName: String

    fun init() {
        val config = ConfigFactory.load().getConfig("mail")
        host = config.getString("host")
        port = config.getInt("port")
        username = config.getString("username")
        password = config.getString("password")
        fromName = config.getString("fromName")
    }
}
