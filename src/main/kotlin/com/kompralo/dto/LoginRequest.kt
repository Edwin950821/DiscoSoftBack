package com.kompralo.dto

data class LoginRequest(
    val username: String? = null,
    val email: String? = null,
    val password: String
) {
    val resolvedEmail: String
        get() = email ?: username ?: throw IllegalArgumentException("Se requiere email o username")
}
