package com.kompralo.dto

import com.kompralo.model.Role

data class AuthResponse(
    val token: String? = null,
    val active: Boolean = true,
    val code: String = "",
    val company_uuid: String? = null,
    val created_at: String? = null,
    val last_modified_at: String? = null,
    val username: String = "",
    val uuid: String = "",
    val image: String? = null,
    val employee_uuid: String? = null,
    val role: String? = null,
    val id: Long? = null,
    val email: String? = null,
    val name: String? = null,
    val subscription: SubscriptionResponse? = null,
    val permissions: List<String>? = null,
    val onboarding_completed: Boolean? = false,
    val default_currency: String? = "COP",
    val twoFactorRequired: Boolean = false,
    val message: String? = null
)

data class SubscriptionResponse(
    val uuid: String,
    val company_uuid: String,
    val plan: String,
    val status: String,
    val trial_start_date: String? = null,
    val trial_end_date: String? = null,
    val current_period_start: String? = null,
    val current_period_end: String? = null,
    val amount: Double? = null,
    val currency: String = "COP",
    val days_remaining: Int? = null,
    val is_active: Boolean = true
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
