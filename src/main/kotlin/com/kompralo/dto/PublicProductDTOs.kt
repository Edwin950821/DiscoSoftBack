package com.kompralo.dto

import java.math.BigDecimal

data class PublicVariantDTO(
    val id: Long,
    val name: String,
    val priceAdjustment: BigDecimal,
    val stock: Int,
    val imageUrl: String?,
)

data class PublicProductResponse(
    val id: Long,
    val name: String,
    val sku: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int,
    val imageUrl: String?,
    val imageUrls: List<String> = emptyList(),
    val variants: List<PublicVariantDTO> = emptyList(),
    val description: String?,
    val sellerId: Long,
    val sellerName: String,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
)
