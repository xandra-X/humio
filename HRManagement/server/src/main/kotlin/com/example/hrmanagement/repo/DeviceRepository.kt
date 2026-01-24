package com.example.hrmanagement.repo

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object DeviceTable : Table("Device") {

    val deviceId = integer("device_id").autoIncrement()
    val userId = integer("user_id")
    val deviceUuid = varchar("device_uuid", 255)
    val fcmToken = varchar("fcm_token", 512).nullable()
    val lastSeen = datetime("last_seen")

    override val primaryKey = PrimaryKey(deviceId)
}

object DeviceRepository {

    /**
     * Register or update a device token
     */
    fun registerDevice(
        userId: Int,
        deviceUuid: String,
        fcmToken: String
    ) = transaction {

        val existing = DeviceTable.select {
            (DeviceTable.userId eq userId) and
                    (DeviceTable.deviceUuid eq deviceUuid)
        }.singleOrNull()

        if (existing == null) {
            DeviceTable.insert {
                it[DeviceTable.userId] = userId
                it[DeviceTable.deviceUuid] = deviceUuid
                it[DeviceTable.fcmToken] = fcmToken
                it[DeviceTable.lastSeen] = LocalDateTime.now()
            }
        } else {
            DeviceTable.update(
                {
                    (DeviceTable.userId eq userId) and
                            (DeviceTable.deviceUuid eq deviceUuid)
                }
            ) {
                it[DeviceTable.fcmToken] = fcmToken
                it[lastSeen] = LocalDateTime.now()
            }
        }
    }

    /**
     * Get all FCM tokens for a user
     */
    fun getTokensForUser(userId: Int): List<String> = transaction {
        DeviceTable
            .slice(DeviceTable.fcmToken)
            .select {
                (DeviceTable.userId eq userId) and
                        DeviceTable.fcmToken.isNotNull()
            }
            .map { it[DeviceTable.fcmToken]!! }
    }
}
