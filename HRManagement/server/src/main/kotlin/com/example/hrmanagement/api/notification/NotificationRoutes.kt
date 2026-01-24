package com.example.hrmanagement.api.notification

import com.example.hrmanagement.model.SimpleResponse
import com.example.hrmanagement.repo.NotificationRepository
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.request.header
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

fun Route.notificationRoutes() {

    route("/api/notifications") {

        get("/unread-count") {
            val userId = resolveUserId(call)
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            val count = NotificationRepository.getUnreadCount(userId)
            call.respond(mapOf("count" to count))
        }

        get {
            val userId = resolveUserId(call)
                ?: return@get call.respond(HttpStatusCode.Unauthorized)

            call.respond(NotificationRepository.getAll(userId))
        }

        post("/read") {
            val userId = resolveUserId(call)
                ?: return@post call.respond(HttpStatusCode.Unauthorized)

            NotificationRepository.markAsRead(userId)
            call.respond(SimpleResponse(true, "Marked as read"))
        }
        post("/test-insert") {

            println("🔥 /test-insert HIT")

            val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")

            NotificationRepository.create(
                userId = userId,
                message = "POSTMAN TEST"
            )

            call.respond(mapOf("success" to true))
        }

    }
}

// ------------------------------------------------------------
// Shared helper
// ------------------------------------------------------------
private fun resolveUserId(call: ApplicationCall): Int? {

    call.request.header("X-User-Id")?.toIntOrNull()?.let {
        return it
    }

    call.request.queryParameters["userId"]?.toIntOrNull()?.let {
        return it
    }

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
