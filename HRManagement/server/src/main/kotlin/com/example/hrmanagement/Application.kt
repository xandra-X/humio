package com.example.hrmanagement

import com.example.hrmanagement.api.auth.authRoutes
import com.example.hrmanagement.api.dashboard.dashboardRoutes
import com.example.hrmanagement.api.attendance.attendanceRoutes
import com.example.hrmanagement.api.device.deviceRoutes
import com.example.hrmanagement.api.leave.leaveRoutes
import com.example.hrmanagement.api.leave.leaveApprovalRoutes
import com.example.hrmanagement.api.notification.notificationRoutes
import com.example.hrmanagement.api.profile.profileRoutes
import com.example.hrmanagement.api.team.teamRoutes
import com.example.hrmanagement.config.DatabaseConfig
import com.example.hrmanagement.repo.AttendanceRepository
import com.example.hrmanagement.repo.DashboardRepository
import com.example.hrmanagement.service.AttendanceService
import com.example.hrmanagement.service.DashboardService
import com.example.hrmanagement.service.TeamService
import com.example.hrmanagement.task.AttendanceAutoCheckoutTask
import com.example.hrmanagement.util.JwtConfig
import com.example.hrmanagement.util.MailConfig
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.typesafe.config.ConfigFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import java.io.File
import java.io.FileInputStream
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.hrmanagement.api.report.reportInboxRoutes


fun main() {

    // 🔥 1️⃣ INITIALIZE FIREBASE ADMIN (REQUIRED)
    val serviceAccount =
        FileInputStream("firebase-service-account.json")

    val firebaseOptions = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    FirebaseApp.initializeApp(firebaseOptions)

    // ------------------------------------------

    val config = ConfigFactory.load()
    val port = config.getInt("ktor.deployment.port")
    val host = "0.0.0.0"

    DatabaseConfig.init()
    JwtConfig.init()
    MailConfig.init()



    embeddedServer(Netty, host = host, port = port) {

        install(CallLogging)

        install(ContentNegotiation) {
            json()
        }
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(
                    JWT.require(Algorithm.HMAC256(JwtConfig.secret))
                        .withIssuer(JwtConfig.issuer)
                        .withAudience(JwtConfig.audience)
                        .build()
                )

                validate { credential ->
                    val payload = credential.payload

                    val userId =
                        payload.getClaim("user_id").asInt()
                            ?: payload.getClaim("userId").asInt()
                            ?: payload.getClaim("id").asInt()
                            ?: payload.getClaim("sub").asString()?.toIntOrNull()

                    if (userId != null) {
                        JWTPrincipal(payload)
                    } else {
                        null
                    }
                }



                realm = JwtConfig.realm
            }
        }



        val uploadsPath = config.getString("app.uploadsPath")

        val dashboardRepo = DashboardRepository()
        val dashboardService = DashboardService(dashboardRepo)

        val attendanceRepo = AttendanceRepository()
        val attendanceService = AttendanceService(attendanceRepo)

        try {
            AttendanceAutoCheckoutTask(attendanceRepo).start()
            environment.log.info("AttendanceAutoCheckoutTask started")
        } catch (t: Throwable) {
            environment.log.warn("Failed to start AttendanceAutoCheckoutTask: ${t.message}")
        }


        routing {

            staticFiles("/uploads", File(uploadsPath)) {
                default("index.html")
            }

            authRoutes()
            reportInboxRoutes()
//            notificationRoutes()
            deviceRoutes()
            dashboardRoutes(dashboardService)
            attendanceRoutes(attendanceService)
            leaveRoutes()
            leaveApprovalRoutes()
            profileRoutes()
            teamRoutes(TeamService())

        }

    }.start(wait = true)
}
