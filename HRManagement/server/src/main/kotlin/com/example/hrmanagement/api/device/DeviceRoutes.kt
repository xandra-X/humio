package com.example.hrmanagement.api.device

import com.example.hrmanagement.model.SimpleResponse
import com.example.hrmanagement.repo.DeviceRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class RegisterDeviceRequest(
    val deviceUuid: String,
    val fcmToken: String
)

fun Route.deviceRoutes() {

    route("/api/device") {

        post("/register") {

            val userId = resolveUserId(call)
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    SimpleResponse(false, "Unauthorized")
                )

            val body = call.receive<RegisterDeviceRequest>()

            DeviceRepository.registerDevice(
                userId = userId,
                deviceUuid = body.deviceUuid,
                fcmToken = body.fcmToken
            )

            call.respond(SimpleResponse(true, "Device registered"))
        }
    }
}

// ------------------------------------------------------------
// Shared helper (same as NotificationRoutes)
// ------------------------------------------------------------
private fun resolveUserId(call: ApplicationCall): Int? {

    call.request.header("X-User-Id")?.toIntOrNull()?.let { return it }

    call.request.queryParameters["userId"]?.toIntOrNull()?.let { return it }

    val auth = call.request.header("Authorization")
        ?.removePrefix("Bearer ")
        ?.trim()

    if (!auth.isNullOrBlank()) {
        try {
            val parts = auth.split(".")
            if (parts.size >= 2) {
                val decoded = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                val json = kotlinx.serialization.json.Json
                    .parseToJsonElement(decoded)
                    .jsonObject

                listOf("userId", "user_id", "id", "sub").forEach { key ->
                    json[key]?.jsonPrimitive?.intOrNull?.let { return it }
                }
            }
        } catch (_: Exception) {
        }
    }

    return null
}
