package com.kompralo.domain.auth.model

import com.kompralo.domain.auth.valueobject.Email
import com.kompralo.domain.auth.valueobject.Password
import com.kompralo.domain.auth.valueobject.Role
import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: Long?,
    val email: Email,
    val password: Password,
    val name: String,
    val role: Role,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    var negocioId: UUID? = null
) {
    init {
        require(name.isNotBlank()) { "Nombre no puede estar vacío" }
    }

    fun isComprador(): Boolean = role == Role.USER

    fun isVendedor(): Boolean = role == Role.BUSINESS

    fun isAdmin(): Boolean = role == Role.ADMIN

    fun canLogin(): Boolean = isActive

    fun withUpdatedPassword(newPassword: Password): User {
        return copy(password = newPassword, updatedAt = LocalDateTime.now())
    }

    fun deactivate(): User {
        return copy(isActive = false, updatedAt = LocalDateTime.now())
    }

    fun activate(): User {
        return copy(isActive = true, updatedAt = LocalDateTime.now())
    }
}
