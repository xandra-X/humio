package com.example.hrmanagement.api.leave

import com.example.hrmanagement.model.SimpleResponse
import com.example.hrmanagement.repo.LeaveRepository
import com.example.hrmanagement.repo.NotificationRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.hrmanagement.repo.AttendanceRepository
import java.time.LocalDate
import com.example.hrmanagement.service.PushService


fun Route.leaveApprovalRoutes() {

    route("/api/hr/leave") {

        post("/{id}/decision") {

            val leaveId = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(SimpleResponse(false, "Invalid leave id"))

            val body = call.receive<Map<String, String>>()
            val decision = body["decision"]
                ?: return@post call.respond(SimpleResponse(false, "Decision is required"))

            if (decision !in listOf("APPROVED", "REJECTED")) {
                return@post call.respond(SimpleResponse(false, "Invalid decision"))
            }

            val userId = LeaveRepository.findUserIdByLeaveId(leaveId)
                ?: return@post call.respond(SimpleResponse(false, "User not found"))

            val ok = when (decision) {
                "APPROVED" -> LeaveRepository.approveLeave(leaveId)
                "REJECTED" -> LeaveRepository.updateLeaveStatus(leaveId, "REJECTED")
                else -> false
            }

            if (!ok) {
                return@post call.respond(SimpleResponse(false, "Leave update failed"))
            }

            // ✅ PUSH + DB SAVE (ONE PLACE ONLY)
            PushService.sendToUser(
                userId = userId,
                title = if (decision == "APPROVED") "Leave Approved" else "Leave Rejected",
                body = "Your leave request has been ${decision.lowercase()}"
            )

            call.respond(SimpleResponse(true, "Leave $decision"))
        }
    }
}
