package com.example.hrmanagement.db.table

import org.jetbrains.exposed.sql.Table

object DepartmentTable : Table("Department") {

    val departmentId = integer("department_id").autoIncrement()
    val name = varchar("name", 150)
    val headEmployeeId = integer("head_employee_id").nullable()

    override val primaryKey = PrimaryKey(departmentId)
}
