package com.example.hrmanagement.service

import com.example.hrmanagement.model.*
import com.example.hrmanagement.repo.UserRepository
import com.example.hrmanagement.repo.PasswordResetRepository
import com.example.hrmanagement.util.JwtTokenGenerator
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.time.LocalDateTime

object AuthService {

    // ============================================================
    // LOGIN
    // ============================================================
    fun login(request: LoginRequest): LoginResponse {
        val user = UserRepository.findByEmail(request.email)
            ?: return LoginResponse(false, "Invalid email")

        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            return LoginResponse(false, "Invalid password")
        }

        // create token including the numeric user id
        val token = JwtTokenGenerator.createToken(user.email, user.id)

        return LoginResponse(true, "Login successful", token)
    }

    // ============================================================
    // FORGOT PASSWORD — GENERATE OTP
    // ============================================================
    fun requestPasswordReset(email: String): SimpleResponse {
        println(">>> [FORGOT] requestPasswordReset called with email='$email'")

        val user = UserRepository.findByEmail(email)
        if (user == null) {
            println(">>> [FORGOT] No user found in DB for email='$email'")
            return SimpleResponse(false, "No account found with that email")
        }

        println(">>> [FORGOT] Found user id=${user.id} email=${user.email}")

        val otp = generateOtp()
        val expiresAt = LocalDateTime.now().plusMinutes(10)
        println(">>> [FORGOT] Generated OTP=$otp expiresAt=$expiresAt")

        PasswordResetRepository.createToken(user.id, otp, expiresAt)
        println(">>> [FORGOT] Stored OTP in password_reset_tokens")

        val sent = EmailService.sendResetCode(email, otp)
        println(">>> [FORGOT] EmailService result: $sent")

        return if (sent) {
            SimpleResponse(true, "Verification code sent to $email")
        } else {
            SimpleResponse(false, "Failed to send verification email")
        }
    }

    // ============================================================
    // VERIFY OTP
    // ============================================================
    fun verifyResetCode(email: String, code: String): SimpleResponse {
        val user = UserRepository.findByEmail(email)
            ?: return SimpleResponse(false, "Invalid email or code")

        println(">>> [VERIFY] email=$email userId=${user.id} code=$code")

        val token = PasswordResetRepository.findValidToken(user.id, code)

        return if (token == null) {
            println(">>> [VERIFY] No valid token found for userId=${user.id}, code=$code")
            SimpleResponse(false, "Invalid or expired code")
        } else {
            println(">>> [VERIFY] Found valid token id=${token.id}")
            SimpleResponse(true, "Code verified")
        }
    }

    // ============================================================
    // RESET PASSWORD
    // ============================================================
    fun resetPassword(email: String, code: String, newPassword: String): SimpleResponse {
        val user = UserRepository.findByEmail(email)
            ?: return SimpleResponse(false, "Invalid email or code")

        val token = PasswordResetRepository.findValidToken(user.id, code)
            ?: return SimpleResponse(false, "Invalid or expired code")

        val newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())

        val updated = UserRepository.updatePassword(user.id, newHash)
        if (!updated) {
            return SimpleResponse(false, "Failed to update password")
        }

        PasswordResetRepository.markUsed(token.id)

        return SimpleResponse(true, "Password updated successfully")
    }

    // ============================================================
    // HELPER: Generate 6-digit OTP
    // ============================================================
    private fun generateOtp(): String {
        val random = SecureRandom()
        val number = random.nextInt(1_000_000)
        return String.format("%06d", number)
    }
}
