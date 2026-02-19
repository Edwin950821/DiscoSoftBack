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
    val access_token: String? = null,   // Access Token de Google OAuth
    val accessToken: String? = null,    // Alternativa camelCase
    val email: String,
    val name: String,
    val googleId: String? = null,       // ID único de Google (sub)
    val sub: String? = null,            // Alternativa para googleId
    val accountType: String,
    val isLogin: Boolean = false        // true = login con Google, false = registro
) {
    // Obtener el token de cualquiera de los dos campos
    fun getToken(): String = access_token ?: accessToken ?: ""

    // Obtener googleId de cualquiera de los dos campos
    fun getGoogleIdValue(): String = googleId ?: sub ?: ""
}