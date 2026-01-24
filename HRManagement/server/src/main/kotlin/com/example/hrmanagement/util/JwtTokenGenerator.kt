package com.example.hrmanagement.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtTokenGenerator {

    /**
     * Create a JWT that includes the user's email and numeric userId.
     * - Adds `userId` claim (numeric) so clients/servers can read a stable id.
     * - Also sets the standard `sub` (subject) to the userId as a string which is
     *   compatible with many libraries that expect `sub`.
     */
    fun createToken(email: String, userId: Int): String {
        val algorithm = Algorithm.HMAC256(JwtConfig.secret)

        return JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .withSubject(userId.toString())          // standard subject (string)
            .withClaim("email", email)
            .withClaim("userId", userId)            // numeric user id claim
            .withExpiresAt(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)) // 7 days
            .sign(algorithm)
    }
}
