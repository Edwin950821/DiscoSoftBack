package com.kompralo.dto

import com.kompralo.model.ClaimResolution
import com.kompralo.model.ClaimStatus
import com.kompralo.model.ClaimType
import java.time.LocalDateTime

data class CreateClaimRequest(
    val orderId: Long,
    val type: ClaimType
)

data class StoreRespondClaimRequest(
    val response: String,
    val newDeliveryDate: String? = null
)

data class AdminResolveClaimRequest(
    val resolution: String,
    val notes: String
)

data class ClaimResponse(
    val id: Long,
    val orderId: Long,
    val orderNumber: String,
    val buyerId: Long,
    val buyerName: String,
    val buyerEmail: String,
    val storeId: Long,
    val storeName: String,
    val type: ClaimType,
    val status: ClaimStatus,
    val estimatedDeliveryDate: LocalDateTime?,
    val claimDate: LocalDateTime,
    val storeResponse: String?,
    val storeResponseDate: LocalDateTime?,
    val resolution: ClaimResolution?,
    val resolvedAt: LocalDateTime?,
    val autoResolved: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ClaimStatsResponse(
    val total: Long,
    val open: Long,
    val inReview: Long,
    val resolved: Long,
    val closed: Long,
    val extended: Long
)
