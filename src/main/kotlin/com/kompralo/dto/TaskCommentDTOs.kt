package com.kompralo.dto

import java.time.LocalDateTime

data class CreateCommentRequest(
    val content: String
)

data class CommentResponse(
    val id: Long,
    val author: TaskUserResponse,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
