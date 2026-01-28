package com.kompralo.dto

data class AuthResponse(
    val token: String?,
    val user: UserResponse,
    val twoFactorRequired: Boolean = false,
    val message: String? = null
)

// DTO para el request de Google OAuth (solo login)
data class GoogleLoginRequest(
    val credential: String // Token JWT de Google
)

// DTO para el request de Google OAuth (registro con selección de tipo de cuenta)
data class GoogleRegisterRequest(
    val credential: String,    // Token JWT de Google
    val accountType: String    // "user" = Comprador | "business" = Vendedor
)

// DTO para el request de Google OAuth usando access_token (alternativo)
data class GoogleRegisterWithTokenRequest(
    val access_token: String,   // Access Token de Google OAuth (match frontend)
    val email: String,          // Email del usuario
    val name: String,           // Nombre del usuario
    val googleId: String,       // ID único de Google (sub)
    val accountType: String     // "user" = Comprador | "business" = Vendedor
)