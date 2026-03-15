package com.kompralo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateCommentRequest(
    @field:NotBlank(message = "El contenido es requerido")
    @field:Size(max = 5000, message = "El comentario no puede exceder 5000 caracteres")
    val content: String
)

data class CommentResponse(
    val id: Long,
    val author: TaskUserResponse,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
