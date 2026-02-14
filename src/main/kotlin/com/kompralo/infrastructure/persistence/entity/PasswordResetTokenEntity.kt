package com.kompralo.infrastructure.persistence.entity

import java.time.LocalDateTime

/**
 * DTO de persistencia (NO es @Entity para evitar conflicto con com.kompralo.model.PasswordResetToken)
 * La entidad JPA principal es com.kompralo.model.PasswordResetToken
 */
data class PasswordResetTokenEntity(
    val id: Long? = null,
    val userId: Long,
    val token: String,
    val expiresAt: LocalDateTime,
    var used: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
