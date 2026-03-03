package com.kompralo.dto

import java.time.LocalDateTime

data class CreateReviewRequest(
    val productId: Long,
    val rating: Int,
    val comment: String? = null,
    val imageUrls: List<String>? = null,
)

data class ReviewResponse(
    val id: Long,
    val productId: Long,
    val buyerId: Long,
    val buyerName: String,
    val rating: Int,
    val comment: String?,
    val imageUrls: List<String>,
    val createdAt: LocalDateTime,
)
