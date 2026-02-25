package com.kompralo.dto

import com.kompralo.model.RequestedSolution
import com.kompralo.model.ReturnReason
import com.kompralo.model.ReturnStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateReturnRequest(
    val orderId: Long,
    val reason: ReturnReason,
    val description: String? = null,
    val imageUrls: List<String> = emptyList(),
    val requestedSolution: RequestedSolution? = null
)

data class ApproveReturnRequest(
    val requiresProductReturn: Boolean = false,
    val refundAmount: BigDecimal
)

data class RejectReturnRequest(
    val reason: String
)

data class AdminResolveReturnRequest(
    val status: ReturnStatus,
    val notes: String? = null
)

data class MarkRefundIssuedRequest(
    val refundMethod: String
)

data class ReturnResponse(
    val id: Long,
    val orderId: Long,
    val orderNumber: String,
    val buyerId: Long,
    val buyerName: String,
    val buyerEmail: String,
    val reason: ReturnReason,
    val description: String?,
    val imageUrls: List<String>,
    val status: ReturnStatus,
    val requestedSolution: String?,
    val storeResponse: String?,
    val adminNotes: String?,
    val requiresProductReturn: Boolean,
    val refundAmount: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val resolvedAt: LocalDateTime?,
    val escalatedAt: LocalDateTime?,
    val refundIssuedAt: LocalDateTime?,
    val refundConfirmedAt: LocalDateTime?,
    val refundMethod: String?
)

data class ReturnStatsResponse(
    val total: Long,
    val pending: Long,
    val inReview: Long,
    val approved: Long,
    val rejected: Long,
    val escalated: Long,
    val refundIssued: Long,
    val completed: Long
)
