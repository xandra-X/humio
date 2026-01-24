package com.example.hrmanagement.repo

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * QRScanRepository
 *
 * - lastScanForDevice(deviceUuid) -> returns most recent scan record (or null)
 * - lastScanForUser(userId) -> returns most recent scan for a user (or null)
 * - logScan(deviceUuid, window, userId = null, attendanceId = null) -> inserts a record
 *
 * This is intentionally lightweight and synchronous (uses Exposed transaction).
 */

object QRScanTable : Table("QRScanLog") {
    val id = integer("id").autoIncrement()
    val deviceUuid = varchar("device_uuid", 255)
    val qrWindow = long("qr_window")          // store the window integer from generator
    val userId = integer("user_id").nullable()
    val attendanceId = integer("attendance_id").nullable()
    val scannedAt = datetime("scanned_at")
    override val primaryKey = PrimaryKey(id)
}

data class QRScanRow(
    val id: Int,
    val deviceUuid: String,
    val qrWindow: Long,
    val userId: Int?,
    val attendanceId: Int?,
    val scannedAt: LocalDateTime
)

object QRScanRepository {

    /**
     * Return the most recent scan row for given device UUID or null if none.
     */
    fun lastScanForDevice(deviceUuid: String): QRScanRow? = transaction {
        QRScanTable.select { QRScanTable.deviceUuid eq deviceUuid }
            .orderBy(QRScanTable.scannedAt, SortOrder.DESC)
            .limit(1)
            .map { row ->
                QRScanRow(
                    id = row[QRScanTable.id],
                    deviceUuid = row[QRScanTable.deviceUuid],
                    qrWindow = row[QRScanTable.qrWindow],
                    userId = row[QRScanTable.userId],
                    attendanceId = row[QRScanTable.attendanceId],
                    scannedAt = row[QRScanTable.scannedAt]
                )
            }.singleOrNull()
    }

    /**
     * Return the most recent scan row for given user id or null if none.
     * This is used to enforce per-user cooldown.
     */
    fun lastScanForUser(userId: Int): QRScanRow? = transaction {
        QRScanTable.select { QRScanTable.userId eq userId }
            .orderBy(QRScanTable.scannedAt, SortOrder.DESC)
            .limit(1)
            .map { row ->
                QRScanRow(
                    id = row[QRScanTable.id],
                    deviceUuid = row[QRScanTable.deviceUuid],
                    qrWindow = row[QRScanTable.qrWindow],
                    userId = row[QRScanTable.userId],
                    attendanceId = row[QRScanTable.attendanceId],
                    scannedAt = row[QRScanTable.scannedAt]
                )
            }.singleOrNull()
    }

    /**
     * Insert a new scan record.
     * Returns the generated id.
     *
     * We accept optional userId and attendanceId so we can link scans to users/attendance rows.
     */
    fun logScan(deviceUuid: String, window: Long, userId: Int? = null, attendanceId: Int? = null): Int = transaction {
        val now = LocalDateTime.now()
        val inserted = QRScanTable.insert { itRow ->
            itRow[QRScanTable.deviceUuid] = deviceUuid
            itRow[QRScanTable.qrWindow] = window
            itRow[QRScanTable.userId] = userId
            itRow[QRScanTable.attendanceId] = attendanceId
            itRow[QRScanTable.scannedAt] = now
        }
        inserted[QRScanTable.id]
    }

    /**
     * (Optional) helper: count scans in the last N seconds for device
     */
    fun countScansInLastSeconds(deviceUuid: String, seconds: Long): Long = transaction {
        val cutoff = LocalDateTime.now().minusSeconds(seconds)
        QRScanTable.select {
            (QRScanTable.deviceUuid eq deviceUuid) and (QRScanTable.scannedAt greaterEq cutoff)
        }.count()
    }
}
