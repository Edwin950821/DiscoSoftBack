package com.kompralo.domain.auth.model

import java.time.LocalDateTime

data class PasswordResetToken(
    val id: Long?,
    val userId: Long,
    val token: String,
    val expiresAt: LocalDateTime,
    val used: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(token.isNotBlank()) { "Token no puede estar vacío" }
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }

    fun isValid(): Boolean {
        return !used && !isExpired()
    }

    fun markAsUsed(): PasswordResetToken {
        require(!used) { "Token ya ha sido usado" }
        return copy(used = true)
    }
}
