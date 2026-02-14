package com.kompralo.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

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
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateProductRequest(
    val name: String,
    val sku: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int = 0,
    val imageUrl: String? = null,
    val description: String? = null
)

data class UpdateProductRequest(
    val name: String? = null,
    val sku: String? = null,
    val category: String? = null,
    val price: BigDecimal? = null,
    val stock: Int? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val status: String? = null
)

data class RestockRequest(
    val productId: Long,
    val quantity: Int,
    val restockDate: LocalDate,
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
