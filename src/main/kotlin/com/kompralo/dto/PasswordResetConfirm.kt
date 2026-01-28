package com.kompralo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO para confirmar recuperación de contraseña con token
 */
data class PasswordResetConfirm(
    @field:NotBlank(message = "El token es requerido")
    val token: String,

    @field:NotBlank(message = "La nueva contraseña es requerida")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val newPassword: String,

    @field:NotBlank(message = "La confirmación de contraseña es requerida")
    val confirmPassword: String
) {
    /**
     * Valida que las contraseñas coincidan
     */
    fun passwordsMatch(): Boolean {
        return newPassword == confirmPassword
    }
}
