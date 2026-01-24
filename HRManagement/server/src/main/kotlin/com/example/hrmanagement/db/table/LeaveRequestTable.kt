package com.example.hrmanagement.db.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object LeaveRequestTable : Table("LeaveRequest") {
    val leaveId = integer("leave_id").autoIncrement()
    val employeeId = integer("employee_id")
    val leaveType = varchar("leave_type", 20)
    val startDate = date("start_date")
    val endDate = date("end_date")
    val days = decimal("days", 6, 2)
    val reason = text("reason").nullable()
    val status = varchar("status", 20)

    override val primaryKey = PrimaryKey(leaveId)
}
