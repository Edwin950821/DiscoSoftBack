package com.kompralo.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class BatchRestockRequest(
    val items: List<BatchItemRequest>,
    val location: String? = null,
    val notes: String? = null,
    val supplier: String? = null,
)

data class BatchItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitCost: BigDecimal? = null,
    val expiryDate: String? = null,
)

data class BatchResponse(
    val id: Long,
    val batchNumber: String,
    val location: String?,
    val notes: String?,
    val supplier: String?,
    val status: String,
    val totalItems: Int,
    val totalQuantity: Int,
    val totalValue: BigDecimal,
    val createdByName: String,
    val createdAt: LocalDateTime,
    val items: List<BatchItemResponse>,
)

data class BatchItemResponse(
    val productId: Long,
    val productName: String,
    val productSku: String,
    val productImageUrl: String?,
    val quantity: Int,
    val previousStock: Int,
    val newStock: Int,
    val unitCost: BigDecimal,
    val expiryDate: String?,
    val quantityRemaining: Int,
    val quantitySold: Int,
    val quantityDamaged: Int,
    val quantityReturned: Int,
)

data class BatchSummaryResponse(
    val id: Long,
    val batchNumber: String,
    val location: String?,
    val supplier: String?,
    val status: String,
    val totalItems: Int,
    val totalQuantity: Int,
    val totalValue: BigDecimal,
    val createdByName: String,
    val createdAt: LocalDateTime,
)

// ==================== Inventory Items (flat list of restocks) ====================

data class InventoryItemResponse(
    val id: Long,
    val batchNumber: String,
    val productId: Long,
    val productName: String,
    val productSku: String,
    val productImageUrl: String?,
    val quantityReceived: Int,
    val quantityRemaining: Int,
    val quantitySold: Int,
    val quantityDamaged: Int,
    val quantityReturned: Int,
    val unitCost: BigDecimal,
    val sellingPrice: BigDecimal,
    val totalCost: BigDecimal,
    val margin: BigDecimal,
    val marginPct: Double,
    val expiryDate: String?,
    val location: String?,
    val supplier: String?,
    val status: String,
    val createdAt: String,
)

// ==================== Inventory Movements ====================

data class InventoryMovementResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productSku: String,
    val movementType: String,
    val quantity: Int,
    val resultingStock: Int,
    val userName: String,
    val referenceType: String?,
    val referenceId: Long?,
    val reason: String?,
    val notes: String?,
    val createdAt: String,
)

// ==================== Stock Adjustment ====================

data class AdjustStockRequest(
    val restockId: Long,
    val quantity: Int,
    val reason: String,
    val notes: String? = null,
)
