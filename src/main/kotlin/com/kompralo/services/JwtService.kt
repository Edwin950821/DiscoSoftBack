package com.kompralo.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private var expiration: Long = 86400000

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(email: String, role: String? = null): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        val builder = Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiryDate)

        if (role != null) {
            builder.claim("role", role)
        }

        return builder
            .signWith(getSigningKey())
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractUsername(token: String): String {
        return extractClaims(token).subject
    }

    private fun extractClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun extractEmailFromToken(token: String): String {
        return extractUsername(token)
    }
}
