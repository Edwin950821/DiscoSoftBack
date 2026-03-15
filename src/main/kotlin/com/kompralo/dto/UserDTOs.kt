package com.kompralo.dto

import com.kompralo.model.Role
import com.kompralo.model.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * DTO para respuesta de usuario (sin password)
 */
data class UserDTO(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(user: User): UserDTO {
            return UserDTO(
                id = user.id!!,
                email = user.email,
                name = user.name,
                role = user.role,
                isActive = user.isUserActive(),
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}

/**
 * DTO para crear un nuevo usuario
 */
data class CreateUserRequest(
    @field:NotBlank(message = "El nombre es requerido")
    @field:Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    val name: String,

    @field:NotBlank(message = "El email es requerido")
    @field:Email(message = "El email debe ser válido")
    val email: String,

    @field:NotBlank(message = "La contraseña es requerida")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val password: String,

    val role: Role = Role.USER,

    val isActive: Boolean = true,

    @field:Size(min = 2, max = 50, message = "El username debe tener entre 2 y 50 caracteres")
    val username: String? = null
)

/**
 * DTO para actualizar un usuario existente
 */
data class UpdateUserRequest(
    @field:Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    val name: String? = null,

    @field:Email(message = "El email debe ser válido")
    val email: String? = null,

    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val password: String? = null,

    val role: Role? = null,

    val isActive: Boolean? = null
)

/**
 * DTO para respuesta paginada
 */
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isFirst: Boolean,
    val isLast: Boolean
)
