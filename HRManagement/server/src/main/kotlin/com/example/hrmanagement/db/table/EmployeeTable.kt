package com.example.hrmanagement.db.table

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal

object EmployeeTable : Table("Employee") {

    val employeeId = integer("employee_id").autoIncrement()
    val employeeCode = varchar("employee_code", 50)   // ✅ FIX
    val hireDate = date("hire_date").nullable()
    val jobTitle = varchar("job_title", 150).nullable()
    val salary = decimal("salary", 12, 2)              // ✅ FIX
    val managerId = integer("manager_id").nullable()
    val departmentId = integer("department_id").nullable()
    val userId = integer("user_id").nullable()
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(employeeId)
}
