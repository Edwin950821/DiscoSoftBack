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

    @Transactional
    fun requestPasswordReset(email: String): Boolean {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("No existe un usuario con este email") }

        if (!user.isActive) {
            throw IllegalArgumentException("Esta cuenta está desactivada")
        }

        tokenRepository.findByUser(user).forEach { token ->
            if (!token.used) {
                token.used = true
                tokenRepository.save(token)
            }
        }

        val token = generateToken()
        val expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours)

        val resetToken = PasswordResetToken(
            user = user,
            token = token,
            expiresAt = expiresAt
        )

        tokenRepository.save(resetToken)

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

    @Transactional
    fun confirmPasswordReset(token: String, newPassword: String) {
        val resetToken = tokenRepository.findByToken(token)
            .orElseThrow { IllegalArgumentException("Token inválido") }

        if (resetToken.used) {
            throw IllegalArgumentException("Este token ya fue utilizado")
        }

        if (resetToken.isExpired()) {
            throw IllegalArgumentException("Este token ha expirado")
        }

        val user = resetToken.user
        user.password = passwordEncoder.encode(newPassword)
        userRepository.save(user)

        resetToken.used = true
        tokenRepository.save(resetToken)

        logger.info("Contraseña reseteada exitosamente para usuario: ${user.email}")
    }

    fun validateToken(token: String): Boolean {
        val resetToken = tokenRepository.findByToken(token)
            .orElse(null) ?: return false

        return resetToken.isValid()
    }

    private fun generateToken(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
}
