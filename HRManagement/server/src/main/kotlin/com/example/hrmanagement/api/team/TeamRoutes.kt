package com.example.hrmanagement.api.team

import com.example.hrmanagement.service.TeamService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.teamRoutes(teamService: TeamService) {

    authenticate("auth-jwt") {
        route("/api/team") {

            get("/my-department") {

                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respondText(
                        "Unauthorized",
                        status = io.ktor.http.HttpStatusCode.Unauthorized
                    )

                val userId = principal.payload
                    .getClaim("userId")
                    .asInt()

                call.respond(teamService.getTeam(userId))
            }
        }
    }
}
