package com.kompralo.infrastructure.persistence.entity

import java.time.LocalDateTime

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
