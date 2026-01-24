package com.example.hrmanagement.api.leave

import com.example.hrmanagement.model.LeaveRequestDto
import com.example.hrmanagement.model.SimpleResponse
import com.example.hrmanagement.service.LeaveService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Base64
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull


fun Route.leaveRoutes(
    leaveService: LeaveService = LeaveService()
) {
    route("/api/leave") {

        get("/balance") {
            val userId = resolveUserId(call)
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            call.respond(leaveService.getLeaveBalance(userId))
        }

        get("/history") {
            val userId = resolveUserId(call)
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            call.respond(leaveService.getLeaveHistory(userId))
        }

        post("/request") {
            try {
                val userId = resolveUserId(call)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val req = try {
                    call.receive<LeaveRequestDto>()
                } catch (e: Exception) {
                    call.application.environment.log.error("Leave parse error", e)
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        SimpleResponse(false, "Invalid request body")
                    )
                }

                val result = leaveService.submitLeave(userId, req)

                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                    result
                )

            } catch (e: Exception) {
                e.printStackTrace() // 🔥 THIS SHOWS THE REAL ERROR
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SimpleResponse(false, e.message ?: "Internal error")
                )
            }
        }


    }
}

// Shared helper to extract userId from request
private fun resolveUserId(call: ApplicationCall): Int? {

    // 1) Header X-User-Id
    call.request.header("X-User-Id")?.toIntOrNull()?.let {
        return it
    }

    // 2) Query param
    call.request.queryParameters["userId"]?.toIntOrNull()?.let {
        return it
    }

    // 3) JWT payload
    val auth = call.request.header("Authorization")
        ?.removePrefix("Bearer ")
        ?.trim()

    if (!auth.isNullOrBlank()) {
        try {
            val parts = auth.split(".")
            if (parts.size >= 2) {
                val decoded = String(Base64.getUrlDecoder().decode(parts[1]))
                val json = Json.parseToJsonElement(decoded).jsonObject

                listOf("userId", "user_id", "id", "sub").forEach { key ->
                    json[key]?.jsonPrimitive?.intOrNull?.let { return it }
                }
            }
        } catch (_: Exception) {
        }
    }

    return null
}
