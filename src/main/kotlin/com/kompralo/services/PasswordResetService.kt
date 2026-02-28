package com.kompralo.services

import com.kompralo.model.PasswordResetToken
import com.kompralo.model.User
import com.kompralo.repository.PasswordResetTokenRepository
import com.kompralo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

/**
 * Servicio para gestión de recuperación de contraseña
 */
@Service
class PasswordResetService(
    private val userRepository: UserRepository,
    private val tokenRepository: PasswordResetTokenRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(PasswordResetService::class.java)

    @Value("\${password-reset.token-expiration-hours}")
    private var tokenExpirationHours: Long = 24

    @Value("\${app.frontend-url:http://localhost:5173}")
    private lateinit var frontendUrl: String

    /**
     * Inicia el proceso de recuperación de contraseña
     * Genera un token y envía email al usuario
     *
     * @param email Email del usuario
     * @return true si se envió el email correctamente
     * @throws IllegalArgumentException si el usuario no existe o no está activo
     */
    @Transactional
    fun requestPasswordReset(email: String): Boolean {
        // Buscar usuario
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("No existe un usuario con este email") }

        // Verificar que el usuario esté activo
        if (!user.isActive) {
            throw IllegalArgumentException("Esta cuenta está desactivada")
        }

        // Invalidar tokens anteriores del usuario
        tokenRepository.findByUser(user).forEach { token ->
            if (!token.used) {
                token.used = true
                tokenRepository.save(token)
            }
        }

        // Generar nuevo token
        val token = generateToken()
        val expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours)

        val resetToken = PasswordResetToken(
            user = user,
            token = token,
            expiresAt = expiresAt
        )

        tokenRepository.save(resetToken)

        // Enviar email
        val userEmail = user.email
        val resetUrl = "$frontendUrl/password-reset"
        val emailSent = emailService.sendPasswordResetEmail(
            to = userEmail,
            userName = user.name,
            resetToken = token,
            resetUrl = resetUrl
        )

        if (emailSent) {
            logger.info("Email de recuperación enviado a: $userEmail")
        } else {
            logger.error("Error al enviar email de recuperación a: $userEmail")
        }

        return emailSent
    }

    /**
     * Confirma el reset de contraseña con el token
     *
     * @param token Token de recuperación
     * @param newPassword Nueva contraseña
     * @throws IllegalArgumentException si el token es inválido o expiró
     */
    @Transactional
    fun confirmPasswordReset(token: String, newPassword: String) {
        // Buscar token
        val resetToken = tokenRepository.findByToken(token)
            .orElseThrow { IllegalArgumentException("Token inválido") }

        // Validar token
        if (resetToken.used) {
            throw IllegalArgumentException("Este token ya fue utilizado")
        }

        if (resetToken.isExpired()) {
            throw IllegalArgumentException("Este token ha expirado")
        }

        // Actualizar contraseña del usuario
        val user = resetToken.user
        user.password = passwordEncoder.encode(newPassword)
        userRepository.save(user)

        // Marcar token como usado
        resetToken.used = true
        tokenRepository.save(resetToken)

        logger.info("Contraseña reseteada exitosamente para usuario: ${user.email}")
    }

    /**
     * Verifica si un token es válido
     *
     * @param token Token a verificar
     * @return true si el token es válido (existe, no usado, no expirado)
     */
    fun validateToken(token: String): Boolean {
        val resetToken = tokenRepository.findByToken(token)
            .orElse(null) ?: return false

        return resetToken.isValid()
    }

    /**
     * Genera un token aleatorio único
     */
    private fun generateToken(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
}
