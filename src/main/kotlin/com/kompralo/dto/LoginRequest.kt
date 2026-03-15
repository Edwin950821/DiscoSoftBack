package com.kompralo.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    val username: String? = null,
    val email: String? = null,
    @field:NotBlank(message = "La contrasena es requerida")
    val password: String
) {
    val resolvedEmail: String
        get() = email ?: username ?: throw IllegalArgumentException("Se requiere email o username")
}
