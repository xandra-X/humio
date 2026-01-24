package com.example.hrmanagement.config

import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {

    fun init() {
        val config = ConfigFactory.load()
        val dbConfig = config.getConfig("ktor.database")

        Database.connect(
            url = dbConfig.getString("url"),
            driver = "com.mysql.cj.jdbc.Driver",
            user = dbConfig.getString("user"),
            password = dbConfig.getString("password")
        )

        println("Connected to MySQL successfully")
    }
}
