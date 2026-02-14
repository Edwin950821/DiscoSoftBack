package com.kompralo.infrastructure.persistence.entity

import java.time.LocalDateTime

/**
 * DTO de persistencia (NO es @Entity para evitar conflicto con com.kompralo.model.User)
 * La entidad JPA principal es com.kompralo.model.User
 */
data class UserEntity(
    val id: Long? = null,
    val email: String,
    var password: String,
    val name: String,
    val role: String,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
