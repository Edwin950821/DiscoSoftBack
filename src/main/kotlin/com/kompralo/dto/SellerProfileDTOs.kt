package com.kompralo.dto

import com.kompralo.model.SellerStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * DTO para registro de vendedor
 * Combina información de usuario + perfil de vendedor
 */
data class SellerRegisterRequest(
    // Datos de cuenta
    @field:NotBlank(message = "El email es requerido")
    val email: String,

    @field:NotBlank(message = "La contraseña es requerida")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val password: String,

    @field:NotBlank(message = "El nombre es requerido")
    val name: String,

    // Datos del negocio
    @field:NotBlank(message = "El nombre del negocio es requerido")
    val businessName: String,

    val businessType: String? = null,

    val description: String? = null,

    val phone: String? = null,

    val website: String? = null,

    val taxId: String? = null,

    // Dirección
    val address: String? = null,

    val city: String? = null,

    val state: String? = null,

    val postalCode: String? = null,

    val country: String = "Colombia"
)

/**
 * DTO para actualizar perfil de vendedor
 */
data class UpdateSellerProfileRequest(
    val businessName: String? = null,

    val businessType: String? = null,

    val description: String? = null,

    val logoUrl: String? = null,

    @field:Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    val phone: String? = null,

    val website: String? = null,

    val taxId: String? = null,

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
 * DTO de respuesta para perfil de vendedor
 */
data class SellerProfileResponse(
    val id: Long,
    val userId: Long,
    val businessName: String,
    val businessType: String?,
    val description: String?,
    val logoUrl: String?,
    val phone: String?,
    val website: String?,
    val taxId: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String,
    val verified: Boolean,
    val verificationDate: String?,
    val status: SellerStatus,
    val totalSales: BigDecimal,
    val totalProducts: Int,
    val averageRating: BigDecimal,
    val totalReviews: Int,
    val createdAt: String,
    val updatedAt: String
)

/**
 * DTO de respuesta para vendedor público (sin datos sensibles)
 */
data class PublicSellerProfileResponse(
    val id: Long,
    val businessName: String,
    val businessType: String?,
    val description: String?,
    val logoUrl: String?,
    val city: String?,
    val state: String?,
    val country: String,
    val verified: Boolean,
    val totalProducts: Int,
    val averageRating: BigDecimal,
    val totalReviews: Int,
    val memberSince: String
)
