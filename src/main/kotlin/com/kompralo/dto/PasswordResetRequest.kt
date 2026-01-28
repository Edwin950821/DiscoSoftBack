package com.kompralo.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * DTO para solicitar recuperación de contraseña
 */
data class PasswordResetRequest(
    @field:NotBlank(message = "El email es requerido")
    @field:Email(message = "El email debe ser válido")
    val email: String
)
