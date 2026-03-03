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
    val auth_provider: String? = null,
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

data class GoogleLoginRequest(
    val credential: String
)

data class GoogleRegisterRequest(
    val credential: String,
    val accountType: String
)

data class UpdateProfilePictureRequest(
    val imageUrl: String
)

data class ChangePasswordRequest(
    val currentPassword: String? = null,
    val newPassword: String
)

data class GoogleRegisterWithTokenRequest(
    val access_token: String? = null,
    val accessToken: String? = null,
    val email: String,
    val name: String,
    val googleId: String? = null,
    val sub: String? = null,
    val accountType: String,
    val isLogin: Boolean = false,
    val picture: String? = null
) {
    fun getToken(): String = access_token ?: accessToken ?: ""

    fun getGoogleIdValue(): String = googleId ?: sub ?: ""
}
