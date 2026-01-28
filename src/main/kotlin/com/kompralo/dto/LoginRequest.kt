package com.kompralo.dto

/**
 * DTO para recibir las credenciales de inicio de sesión desde el frontend
 *
 * Campos que espera recibir en el JSON:
 * {
 *   "email": "sanjuanedwin95@gmail.com",
 *   "password": "12345"
 * }
 */
data class LoginRequest(
    val email: String,      // Email del usuario
    val password: String    // Contraseña en texto plano
)
