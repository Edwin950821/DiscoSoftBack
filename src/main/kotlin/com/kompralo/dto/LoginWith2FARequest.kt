package com.kompralo.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * DTO para login con código 2FA
 */
data class LoginWith2FARequest(
    @field:NotBlank(message = "El email es requerido")
    @field:Email(message = "El email debe ser válido")
    val email: String,

    @field:NotBlank(message = "La contraseña es requerida")
    val password: String,

    @field:NotBlank(message = "El código 2FA es requerido")
    @field:Pattern(regexp = "^[0-9]{6,8}$", message = "El código debe ser de 6-8 dígitos")
    val twoFactorCode: String
)
