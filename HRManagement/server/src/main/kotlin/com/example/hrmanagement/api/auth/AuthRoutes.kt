package com.example.hrmanagement.api.auth

import com.example.hrmanagement.model.*
import com.example.hrmanagement.service.AuthService
import com.example.hrmanagement.service.EmailService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {

    route("/api/auth") {

        // ------------------------------------------------------------
        // LOGIN
        // ------------------------------------------------------------
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = AuthService.login(request)
            call.respond(response)
        }

        // ------------------------------------------------------------
        // FORGOT PASSWORD → Send OTP
        // ------------------------------------------------------------
        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            val result = AuthService.requestPasswordReset(request.email)

            val status =
                if (result.success) HttpStatusCode.OK
                else HttpStatusCode.BadRequest

            call.respond(status, result)
        }

        // ------------------------------------------------------------
        // VERIFY OTP
        // ------------------------------------------------------------
        post("/verify-reset-code") {
            val request = call.receive<VerifyResetCodeRequest>()
            val result = AuthService.verifyResetCode(
                request.email,
                request.code
            )

            val status =
                if (result.success) HttpStatusCode.OK
                else HttpStatusCode.BadRequest

            call.respond(status, result)
        }

        // ------------------------------------------------------------
        // RESET PASSWORD
        // ------------------------------------------------------------
        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            val result = AuthService.resetPassword(
                request.email,
                request.code,
                request.newPassword
            )

            val status =
                if (result.success) HttpStatusCode.OK
                else HttpStatusCode.BadRequest

            call.respond(status, result)
        }

        // ------------------------------------------------------------
        // TEST EMAIL (debug helper)
        // ------------------------------------------------------------
        post("/test-email") {
            val req = call.receive<TestEmailRequest>()
            val ok = EmailService.sendResetCode(req.email, "123456")

            if (ok) {
                call.respond(
                    HttpStatusCode.OK,
                    SimpleResponse(true, "Test email sent to ${req.email}")
                )
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SimpleResponse(false, "Failed to send test email")
                )
            }
        }
    }
}
