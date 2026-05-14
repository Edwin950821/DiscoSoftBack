package com.kompralo.infrastructure.persistence.entity

import java.time.LocalDateTime

data class PasswordResetTokenEntity(
    val id: Long? = null,
    val userId: Long,
    val token: String,
    val expiresAt: LocalDateTime,
    var used: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
