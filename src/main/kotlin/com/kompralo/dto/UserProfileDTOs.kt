package com.kompralo.dto

import jakarta.validation.constraints.Size

/**
 * DTO para actualizar perfil de usuario
 */
data class UpdateUserProfileRequest(
    @field:Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    val phone: String? = null,

    val avatarUrl: String? = null,

    val address: String? = null,

    @field:Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    val city: String? = null,

    @field:Size(max = 100, message = "El estado no puede exceder 100 caracteres")
    val state: String? = null,

    @field:Size(max = 20, message = "El código postal no puede exceder 20 caracteres")
    val postalCode: String? = null,

    @field:Size(max = 100, message = "El país no puede exceder 100 caracteres")
    val country: String? = null
)

/**
 * DTO de respuesta para perfil de usuario
 */
data class UserProfileResponse(
    val id: Long,
    val userId: Long,
    val phone: String?,
    val avatarUrl: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String,
    val createdAt: String,
    val updatedAt: String
)
