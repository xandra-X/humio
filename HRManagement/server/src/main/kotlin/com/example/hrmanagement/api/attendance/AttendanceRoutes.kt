package com.example.hrmanagement.api.attendance

import com.example.hrmanagement.data.AttendanceHistoryItem
import com.example.hrmanagement.data.AttendanceTodayResponse
import com.example.hrmanagement.model.CheckInOutRequest
import com.example.hrmanagement.model.SimpleResponse
import com.example.hrmanagement.service.AttendanceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Base64

fun Route.attendanceRoutes(attendanceService: AttendanceService) {

    route("/api/attendance") {

        // GET /api/attendance/today
        get("/today") {
            val userId = resolveUserId(call)
            val today: AttendanceTodayResponse =
                attendanceService.getTodayAttendance(userId)
            call.respond(HttpStatusCode.OK, today)
        }

        // POST /api/attendance/check
        post("/check") {
            val userId = resolveUserId(call)
            val body = call.receive<CheckInOutRequest>()
            val result: SimpleResponse =
                attendanceService.handleCheck(userId, body)

            val status =
                if (result.success) HttpStatusCode.OK
                else HttpStatusCode.BadRequest

            call.respond(status, result)
        }

        // GET /api/attendance/history
        get("/history") {
            val userId = resolveUserId(call)
            val limit =
                call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

            val list: List<AttendanceHistoryItem> =
                attendanceService.getHistory(userId, limit)

            call.respond(HttpStatusCode.OK, list)
        }
    }
}

// ------------------------------------------------------------
// Shared helper
// ------------------------------------------------------------
private fun resolveUserId(call: ApplicationCall): Int {

    // 1) Header
    call.request.header("X-User-Id")?.toIntOrNull()?.let {
        return it
    }

    // 2) Query param
    call.request.queryParameters["userId"]?.toIntOrNull()?.let {
        return it
    }

    // 3) JWT payload
    val auth =
        call.request.header("Authorization")
            ?.removePrefix("Bearer")
            ?.trim()

    if (!auth.isNullOrBlank()) {
        try {
            val parts = auth.split(".")
            if (parts.size >= 2) {
                val decoded =
                    String(Base64.getUrlDecoder().decode(parts[1]))
                val json =
                    Json.parseToJsonElement(decoded).jsonObject

                listOf("userId", "user_id", "id", "sub").forEach { key ->
                    json[key]?.toString()
                        ?.trim('"')
                        ?.toIntOrNull()
                        ?.let { return it }
                }
            }
        } catch (e: Exception) {
            call.application.environment.log.debug(
                "JWT parse failed: ${e.message}"
            )
        }
    }

    // Fallback (local testing)
    return 1
}
