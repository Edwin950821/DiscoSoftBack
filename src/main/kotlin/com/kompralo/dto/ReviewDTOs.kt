package com.kompralo.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateReviewRequest(
    val productId: Long,
    @field:Min(value = 1, message = "La calificacion minima es 1")
    @field:Max(value = 5, message = "La calificacion maxima es 5")
    val rating: Int,
    @field:Size(max = 2000, message = "El comentario no puede exceder 2000 caracteres")
    val comment: String? = null,
    @field:Size(max = 5, message = "Maximo 5 imagenes")
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
