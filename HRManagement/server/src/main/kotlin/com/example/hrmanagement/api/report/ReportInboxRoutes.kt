package com.example.hrmanagement.api.report

import com.example.hrmanagement.db.table.ReportInboxTable
import com.example.hrmanagement.repo.ReportInboxRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

import kotlinx.serialization.Serializable

@Serializable
data class ReportRequest(
    val title: String,
    val message: String
)

fun Route.reportInboxRoutes() {

    authenticate("auth-jwt") {

        route("/api/reports") {

            // =========================
            // EMPLOYEE / HR submit report
            // =========================
            post("/inbox") {

                val principal = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val payload = principal.payload

                val userId =
                    payload.getClaim("user_id").asInt()
                        ?: payload.getClaim("id").asInt()
                        ?: payload.getClaim("sub").asString()?.toIntOrNull()

                val role =
                    payload.getClaim("user_type").asString()
                        ?: payload.getClaim("role").asString()
                        ?: "EMPLOYEE"

                if (userId == null) {
                    return@post call.respond(HttpStatusCode.Unauthorized)
                }


                val body = call.receive<ReportRequest>()

                ReportInboxRepository.create(
                    senderId = userId,
                    senderRole = role,
                    title = body.title,
                    message = body.message
                )

                call.respond(HttpStatusCode.Created, mapOf("success" to true))
            }

            // =========================
            // MANAGER inbox list
            // =========================
            get("/inbox") {

                val rows = ReportInboxRepository.findAll()

                call.respond(
                    rows.map {
                        mapOf(
                            "report_id" to it[ReportInboxTable.reportId],
                            "sender_id" to it[ReportInboxTable.senderId],
                            "sender_role" to it[ReportInboxTable.senderRole],
                            "title" to it[ReportInboxTable.title],
                            "status" to it[ReportInboxTable.status],
                            "created_at" to it[ReportInboxTable.createdAt].toString()
                        )
                    }
                )
            }

            // =========================
            // MANAGER view detail
            // =========================
            get("/inbox/{id}") {

                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val r = ReportInboxRepository.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(
                    mapOf(
                        "report_id" to r[ReportInboxTable.reportId],
                        "sender_id" to r[ReportInboxTable.senderId],
                        "sender_role" to r[ReportInboxTable.senderRole],
                        "title" to r[ReportInboxTable.title],
                        "message" to r[ReportInboxTable.message],
                        "status" to r[ReportInboxTable.status],
                        "created_at" to r[ReportInboxTable.createdAt].toString()
                    )
                )
            }
        }
    }
}
