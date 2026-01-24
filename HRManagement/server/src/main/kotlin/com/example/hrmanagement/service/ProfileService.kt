package com.example.hrmanagement.service

import com.example.hrmanagement.repo.ProfileRepository
import com.example.hrmanagement.model.*
import com.example.hrmanagement.repo.ProfileRepository.getOvertimeForEmployee
import org.jetbrains.exposed.sql.transactions.transaction

class ProfileService {

    fun getProfile(userId: Int): ProfileResponse {

        val baseProfile = ProfileRepository.getProfile(userId)

        // Get employeeId
        val employeeId = ProfileRepository.getEmployeeIdByUserId(userId)
            ?: return baseProfile

        // Fetch overtime list
        val overtimeList = getOvertimeForEmployee(employeeId)

        val totalOvertimePay = overtimeList.sumOf { it.amount }

        return baseProfile.copy(
            pay = baseProfile.pay.copy(
                totalOvertimePay = totalOvertimePay
            ),
            overtime = overtimeList
        )
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
