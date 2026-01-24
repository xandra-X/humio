package com.example.hrmanagement.api.dashboard

import com.example.hrmanagement.model.CheckInOutRequest
import com.example.hrmanagement.service.DashboardService
import io.ktor.server.application.*   // ✅ brings in ApplicationCall + call
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.Base64

fun Route.dashboardRoutes(dashboardService: DashboardService) {

    route("/api/dashboard") {

        // GET /api/dashboard
        get {
            val userId = resolveUserId(call)
            val overview = dashboardService.getDashboard(userId)
            call.respond(overview)
        }

        // POST /api/dashboard/check
        post("/check") {
            val userId = resolveUserId(call)
            val body = call.receive<CheckInOutRequest>()
            val result = dashboardService.handleCheck(userId, body)
            call.respond(result)
        }
    }
}

/**
 * Resolve user id from the request by checking:
 *  1) X-User-Id header
 *  2) userId query parameter
 *  3) Authorization: Bearer <jwt> (parse payload JSON for id claims)
 *  4) fallback to 1
 */
private fun resolveUserId(call: ApplicationCall): Int {

    // 1) Header
    call.request.header("X-User-Id")?.toIntOrNull()?.let {
        return it
    }

    // 2) Query param
    call.request.queryParameters["userId"]?.toIntOrNull()?.let {
        return it
    }

    // 3) JWT payload decode
    val auth = call.request.header("Authorization")
        ?.removePrefix("Bearer")
        ?.trim()

    if (!auth.isNullOrBlank()) {
        try {
            val parts = auth.split(".")
            if (parts.size >= 2) {
                val decoded = String(Base64.getUrlDecoder().decode(parts[1]))
                val json = Json.parseToJsonElement(decoded).jsonObject

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

    // 4) fallback
    return 1
}
