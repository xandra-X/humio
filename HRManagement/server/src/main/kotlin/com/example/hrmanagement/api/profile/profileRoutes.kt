package com.example.hrmanagement.api.profile

import com.example.hrmanagement.service.ProfileService
import io.ktor.server.application.*
import io.ktor.server.request.header
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.util.Base64

fun Route.profileRoutes() {

    route("/api/profile") {
        get {

            val userId = resolveUserId(call)
                ?: return@get call.respond(
                    io.ktor.http.HttpStatusCode.Unauthorized,
                    mapOf("message" to "Unauthorized")
                )

            val result = ProfileService().getProfile(userId)
            println("PROFILE API RESULT = $result")
            call.respond(result)

        }
    }
}

/**
 * Same helper logic you already use elsewhere
 */
private fun resolveUserId(call: ApplicationCall): Int? {

    call.request.header("X-User-Id")?.toIntOrNull()?.let { return it }

    val auth = call.request.header("Authorization")
        ?.removePrefix("Bearer ")
        ?.trim()

    if (!auth.isNullOrBlank()) {
        try {
            val payload = auth.split(".").getOrNull(1) ?: return null
            val json = Json
                .parseToJsonElement(String(Base64.getUrlDecoder().decode(payload)))
                .jsonObject

            listOf("userId", "user_id", "id", "sub").forEach { key ->
                json[key]?.jsonPrimitive?.content
                    ?.toIntOrNull()
                    ?.let { return it }

            }
        } catch (_: Exception) {}
    }

    return null
}
