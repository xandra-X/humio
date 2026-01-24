package com.example.hrmanagement.db.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object AttendanceTable : Table("Attendance") {

    val attendanceId = integer("attendance_id").autoIncrement()
    val employeeId = integer("employee_id")
    val date = date("date")

    val checkIn = datetime("check_in").nullable()
    val checkOut = datetime("check_out").nullable()

    val status = varchar("status", 20)
    val sourceCol = varchar("source", 20)

    val updatedAt = datetime("updated_at")
    val autoCheckedOut = bool("auto_checked_out")
    val sourceDetails = varchar("source_details", 64).nullable()

    override val primaryKey = PrimaryKey(attendanceId)
}
