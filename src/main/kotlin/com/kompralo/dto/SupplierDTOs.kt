package com.kompralo.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateSupplierRequest(
    val name: String,
    val nit: String? = null,
    val contactName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val notes: String? = null
)

data class UpdateSupplierRequest(
    val name: String? = null,
    val nit: String? = null,
    val contactName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val notes: String? = null
)

data class SupplierResponse(
    val id: Long,
    val name: String,
    val nit: String?,
    val contactName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val notes: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class SupplierSummaryResponse(
    val id: Long,
    val name: String,
    val nit: String?,
    val isActive: Boolean
)

data class SupplierStatsResponse(
    val totalSuppliers: Int,
    val activeSuppliers: Int,
    val totalPurchaseValue: BigDecimal,
    val totalBatches: Int
)

data class SupplierMetricResponse(
    val supplierId: Long,
    val supplierName: String,
    val totalBatches: Int,
    val totalQuantity: Int,
    val totalValue: BigDecimal,
    val avgBatchValue: BigDecimal,
    val lastPurchaseDate: LocalDateTime?,
    val productCount: Int
)

data class SupplierPurchaseHistoryResponse(
    val batchId: Long,
    val batchNumber: String,
    val totalItems: Int,
    val totalQuantity: Int,
    val totalValue: BigDecimal,
    val createdAt: LocalDateTime
)
