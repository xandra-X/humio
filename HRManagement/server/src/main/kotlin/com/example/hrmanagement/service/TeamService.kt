package com.example.hrmanagement.service

import com.example.hrmanagement.model.*
import com.example.hrmanagement.db.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and

class TeamService {

    fun getTeam(userId: Int): TeamResponse = transaction {

        val today = LocalDate.now()

        // ================= LOGGED-IN EMPLOYEE =================
        val employee = EmployeeTable
            .select { EmployeeTable.userId eq userId }
            .singleOrNull()
            ?: error("Employee not found")

        val departmentId = employee[EmployeeTable.departmentId]
            ?: error("Employee has no department")

        val supervisorId = employee[EmployeeTable.managerId]

        // ================= DEPARTMENT =================
        val department = DepartmentTable
            .select { DepartmentTable.departmentId eq departmentId }
            .single()

        // ================= TODAY ATTENDANCE =================
        val attendanceMap = AttendanceTable
            .select { AttendanceTable.date eq today }
            .associateBy { it[AttendanceTable.employeeId] }

        // ================= SUPERVISOR =================
        val supervisor = supervisorId?.let { managerId ->
            EmployeeTable
                .join(UserTable, JoinType.INNER, EmployeeTable.userId, UserTable.userId)
                .select { EmployeeTable.employeeId eq managerId }
                .singleOrNull()
                ?.let {

                    val attendance = attendanceMap[managerId]
                    val status = attendance?.get(AttendanceTable.status)?.toString()

                    TeamMemberDto(
                        employeeId = managerId,
                        employeeCode = it[EmployeeTable.employeeCode],
                        fullName = it[UserTable.fullName] ?: "",
                        jobTitle = it[EmployeeTable.jobTitle],
                        email = it[UserTable.email],
                        avatarUrl = it[UserTable.profileImage],
                        phone = null,
                        active = status == "PRESENT" || status == "LATE"
                    )
                }
        }

        // ================= TEAM MEMBERS =================
        val condition = if (supervisorId != null) {
            (EmployeeTable.departmentId eq departmentId) and
                    (EmployeeTable.employeeId neq supervisorId)
        } else {
            EmployeeTable.departmentId eq departmentId
        }

        val members = EmployeeTable
            .join(UserTable, JoinType.INNER, EmployeeTable.userId, UserTable.userId)
            .select { condition }
            .map {

                val empId = it[EmployeeTable.employeeId]
                val attendance = attendanceMap[empId]
                val status = attendance?.get(AttendanceTable.status)?.toString()

                TeamMemberDto(
                    employeeId = empId,
                    employeeCode = it[EmployeeTable.employeeCode],
                    fullName = it[UserTable.fullName] ?: "",
                    jobTitle = it[EmployeeTable.jobTitle],
                    email = it[UserTable.email],
                    avatarUrl = it[UserTable.profileImage],
                    phone = null,
                    active = status == "PRESENT" || status == "LATE"
                )
            }

        // ================= RESPONSE =================
        TeamResponse(
            department = DepartmentDto(
                id = departmentId,
                name = department[DepartmentTable.name]
            ),
            head = supervisor,
            members = members
        )
    }
}
