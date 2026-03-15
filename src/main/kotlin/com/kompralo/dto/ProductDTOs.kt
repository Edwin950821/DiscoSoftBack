package com.kompralo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ProductVariantDTO(
    val id: Long? = null,
    @field:NotBlank(message = "El nombre de la variante es requerido")
    @field:Size(max = 255, message = "El nombre de la variante no puede exceder 255 caracteres")
    val name: String,
    @field:Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
    val sku: String? = null,
    val priceAdjustment: BigDecimal = BigDecimal.ZERO,
    @field:PositiveOrZero(message = "El stock no puede ser negativo")
    val stock: Int = 0,
    val imageUrl: String? = null,
    val active: Boolean = true,
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val sku: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int,
    val sales: Int,
    val status: String,
    val imageUrl: String?,
    val imageUrls: List<String> = emptyList(),
    val variants: List<ProductVariantDTO> = emptyList(),
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateProductRequest(
    @field:NotBlank(message = "El nombre del producto es requerido")
    @field:Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    val name: String,
    @field:NotBlank(message = "El SKU es requerido")
    @field:Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
    val sku: String,
    @field:NotBlank(message = "La categoria es requerida")
    val category: String,
    @field:Positive(message = "El precio debe ser mayor a cero")
    val price: BigDecimal,
    @field:PositiveOrZero(message = "El stock no puede ser negativo")
    val stock: Int = 0,
    val imageUrl: String? = null,
    @field:Size(max = 10, message = "Maximo 10 imagenes")
    val imageUrls: List<String>? = null,
    val variants: List<ProductVariantDTO>? = null,
    @field:Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
    val description: String? = null
)

data class UpdateProductRequest(
    @field:Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    val name: String? = null,
    @field:Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
    val sku: String? = null,
    val category: String? = null,
    @field:Positive(message = "El precio debe ser mayor a cero")
    val price: BigDecimal? = null,
    @field:PositiveOrZero(message = "El stock no puede ser negativo")
    val stock: Int? = null,
    val imageUrl: String? = null,
    @field:Size(max = 10, message = "Maximo 10 imagenes")
    val imageUrls: List<String>? = null,
    val variants: List<ProductVariantDTO>? = null,
    @field:Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
    val description: String? = null,
    val status: String? = null
)

data class RestockRequest(
    val productId: Long,
    @field:Positive(message = "La cantidad debe ser mayor a cero")
    val quantity: Int,
    val restockDate: LocalDate,
    @field:Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    val notes: String? = null
)

data class RestockResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val previousStock: Int,
    val newStock: Int,
    val restockDate: LocalDate,
    val notes: String?,
    val createdAt: LocalDateTime
)
