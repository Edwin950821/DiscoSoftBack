package com.kompralo.dto

import java.math.BigDecimal

data class PublicProductResponse(
    val id: Long,
    val name: String,
    val sku: String,
    val category: String,
    val price: BigDecimal,
    val stock: Int,
    val imageUrl: String?,
    val description: String?,
    val sellerId: Long,
    val sellerName: String,
)
