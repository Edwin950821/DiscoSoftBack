package com.kompralo.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

/**
 * Servicio para gestión de JSON Web Tokens (JWT)
 * Genera, valida y extrae información de tokens de autenticación
 */
@Service
class JwtService {

    // Secret key desde application.properties
    @Value("\${jwt.secret}")
    private lateinit var secret: String

    // Tiempo de expiración en milisegundos (default: 24 horas)
    @Value("\${jwt.expiration}")
    private var expiration: Long = 86400000

    /**
     * Genera la clave de firma HMAC-SHA desde el secret
     */
    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /**
     * Genera un token JWT para el usuario
     * @param email Email del usuario (usado como subject)
     * @param role Rol del usuario (USER, BUSINESS, ADMIN)
     * @return Token JWT firmado
     */
    fun generateToken(email: String, role: String? = null): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        val builder = Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiryDate)

        // Agregar el rol como claim si está presente
        if (role != null) {
            builder.claim("role", role)
        }

        return builder
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * Valida si un token es válido
     * @param token Token JWT a validar
     * @return true si el token es válido, false en caso contrario
     */
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

    /**
     * Extrae el email (username) del token
     * @param token Token JWT
     * @return Email del usuario
     */
    fun extractUsername(token: String): String {
        return extractClaims(token).subject
    }

    /**
     * Extrae los claims (datos) del token
     */
    private fun extractClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * Alias para extractUsername (más semántico)
     */
    fun extractEmailFromToken(token: String): String {
        return extractUsername(token)
    }
}