package com.kompralo.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class BatchRestockRequest(
    val items: List<BatchItemRequest>,
    val location: String? = null,
    val notes: String? = null
)

data class BatchItemRequest(
    val productId: Long,
    val quantity: Int
)

data class BatchResponse(
    val id: Long,
    val batchNumber: String,
    val location: String?,
    val notes: String?,
    val totalItems: Int,
    val totalQuantity: Int,
    val totalValue: BigDecimal,
    val createdByName: String,
    val createdAt: LocalDateTime,
    val items: List<BatchItemResponse>
)

data class BatchItemResponse(
    val productId: Long,
    val productName: String,
    val productSku: String,
    val productImageUrl: String?,
    val quantity: Int,
    val previousStock: Int,
    val newStock: Int
)

data class BatchSummaryResponse(
    val id: Long,
    val batchNumber: String,
    val location: String?,
    val totalItems: Int,
    val totalQuantity: Int,
    val totalValue: BigDecimal,
    val createdByName: String,
    val createdAt: LocalDateTime
)
