package com.example.hrmanagement.repo

import com.example.hrmanagement.model.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet
import com.typesafe.config.ConfigFactory

object ProfileRepository {

    fun getProfile(userId: Int): ProfileResponse {

        return transaction {
            val baseUrl = ConfigFactory.load().getString("app.baseUrl")
            var profileResponse: ProfileResponse? = null

            val sql = """
    SELECT 
        u.full_name,
        u.email,
        u.profile_image,
        u.user_type,
        e.employee_code,
        e.salary,
        e.shift_name,
        d.name AS department_name,
        s.start_time AS start_time,
        s.end_time AS end_time
    FROM Employee e
    JOIN User u ON e.user_id = u.user_id
    LEFT JOIN Department d ON e.department_id = d.department_id
    LEFT JOIN Shift s ON s.name = e.shift_name
    WHERE u.user_id = $userId
    LIMIT 1
""".trimIndent()


            // ✅ exec must be called on Transaction (this)
            this.exec(sql) { rs: ResultSet ->
                if (rs.next()) {
                    val monthlySalary = rs.getBigDecimal("salary")?.toDouble() ?: 0.0
                    val annualSalary = monthlySalary * 12
                    val rawAvatar = rs.getString("profile_image")
                    val shiftName = rs.getString("shift_name") ?: "N/A"

                    val startTime = rs.getTime("start_time")?.toString()?.substring(0, 5)
                    val endTime = rs.getTime("end_time")?.toString()?.substring(0, 5)

                    val shiftDisplay = if (startTime != null && endTime != null) {
                        "$shiftName ($startTime - $endTime)"
                    } else {
                        shiftName
                    }


                    val avatarUrl = rawAvatar
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            when {
                                it.startsWith("http") -> it
                                it.startsWith("/uploads") -> baseUrl.trimEnd('/') + it

                                else -> "$baseUrl/uploads/profile_images/$it"
                            }
                        }



                    profileResponse = ProfileResponse(
                        profile = ProfileInfo(
                            fullName = rs.getString("full_name"),
                            employeeCode = rs.getString("employee_code"),
                            email = rs.getString("email"),
                            department = rs.getString("department_name") ?: "N/A",
                            role = rs.getString("user_type"),
                            shift = shiftDisplay,
                            avatarUrl = avatarUrl
                        ),
                        pay = PayInfo(
                            annualSalary = monthlySalary * 12,
                            monthlySalary = monthlySalary,
                            totalOvertimePay = 0.0
                        ),
                        overtime = emptyList()
                    )

                }
            }

            // ✅ transaction must return a NON-null value
            profileResponse ?: ProfileResponse(
                profile = ProfileInfo(
                    fullName = "Unknown",
                    employeeCode = "N/A",
                    email = "",
                    department = "N/A",
                    role = "EMPLOYEE",
                    shift = "N/A",
                    avatarUrl = null
                ),
                pay = PayInfo(
                    annualSalary = 0.0,
                    monthlySalary = 0.0,
                    totalOvertimePay = 0.0
                ),
                overtime = emptyList()

            )
        }
    }
    fun getEmployeeIdByUserId(userId: Int): Int? {
        return transaction {
            var employeeId: Int? = null

            exec(
                "SELECT employee_id FROM Employee WHERE user_id = $userId LIMIT 1"
            ) { rs ->
                if (rs.next()) {
                    employeeId = rs.getInt("employee_id")
                }
            }
            employeeId
        }
    }



    fun getOvertimeForEmployee(employeeId: Int): List<OvertimeItem> {
        return transaction {
            exec(
                """
            SELECT 
                overtime_date,
                hours,
                overtime_pay
            FROM v_employee_overtime
            WHERE employee_id = $employeeId
            ORDER BY overtime_date DESC
            """
            ) { rs ->
                val list = mutableListOf<OvertimeItem>()
                while (rs.next()) {
                    list.add(
                        OvertimeItem(
                            date = rs.getDate("overtime_date").toString(),
                            hours = rs.getDouble("hours"),
                            amount = rs.getDouble("overtime_pay"),
                            status = "ASSIGNED"
                        )
                    )
                }
                list
            } ?: emptyList()
        }
    }


}
