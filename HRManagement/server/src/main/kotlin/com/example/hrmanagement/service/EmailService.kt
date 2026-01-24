package com.example.hrmanagement.service

import com.example.hrmanagement.util.MailConfig
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

object EmailService {

    fun sendResetCode(email: String, code: String): Boolean {
        return try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", MailConfig.host)       // smtp.gmail.com
                put("mail.smtp.port", MailConfig.port.toString()) // 587
                put("mail.smtp.ssl.trust", MailConfig.host)
            }

            val session = Session.getInstance(props, object : jakarta.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(MailConfig.username, MailConfig.password)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(MailConfig.username, MailConfig.fromName))
                setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(email, false)
                )
                subject = "Your HR Management Password Reset Code"

                setText(
                    """
                    Hello,

                    Your password reset verification code is: $code

                    This code will expire in 10 minutes.
                    If you did not request a password reset, simply ignore this email.

                    — HR Management System
                    """.trimIndent()
                )
            }

            Transport.send(message)
            println(">>> [EMAIL] Sent password reset code $code to $email")

            true   // <--- returning success

        } catch (e: Exception) {
            println(">>> [EMAIL ERROR] Failed to send email: ${e.message}")
            false  // <--- reporting failure
        }
    }
}
