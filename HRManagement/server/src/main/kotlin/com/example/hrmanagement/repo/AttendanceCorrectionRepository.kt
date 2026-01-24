package com.example.hrmanagement.repo

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object AttendanceCorrectionTable : Table("AttendanceCorrection") {
    val correctionId = integer("correction_id").autoIncrement()
    val attendanceId = integer("attendance_id")
    val requestedBy = integer("requested_by").nullable()
    val reason = text("reason").nullable()
    val proposedCheckIn = datetime("proposed_check_in").nullable()
    val proposedCheckOut = datetime("proposed_check_out").nullable()
    val status = varchar("status", 20)
    val submittedAt = datetime("submitted_at")
}

object AttendanceCorrectionRepository {
    fun createCorrection(attendanceId: Int?, requestedBy: Int?, reason: String?, proposedCheckIn: LocalDateTime?, proposedCheckOut: LocalDateTime?): Boolean {
        return try {
            transaction {
                AttendanceCorrectionTable.insert {
                    it[AttendanceCorrectionTable.attendanceId] = attendanceId ?: 0
                    it[AttendanceCorrectionTable.requestedBy] = requestedBy
                    it[AttendanceCorrectionTable.reason] = reason
                    it[AttendanceCorrectionTable.proposedCheckIn] = proposedCheckIn
                    it[AttendanceCorrectionTable.proposedCheckOut] = proposedCheckOut
                    it[AttendanceCorrectionTable.status] = "PENDING"
                    it[AttendanceCorrectionTable.submittedAt] = LocalDateTime.now()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
