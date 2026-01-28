package com.kompralo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * DTO para verificar código 2FA
 */
data class TwoFactorVerifyRequest(
    @field:NotBlank(message = "El código es requerido")
    @field:Pattern(regexp = "^[0-9]{6}$", message = "El código debe ser de 6 dígitos")
    val code: String
)
