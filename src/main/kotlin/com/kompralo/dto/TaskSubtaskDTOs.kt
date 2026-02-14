package com.kompralo.dto

import java.time.LocalDateTime

data class CreateSubtaskRequest(
    val title: String
)

data class UpdateSubtaskRequest(
    val title: String? = null,
    val completed: Boolean? = null,
    val sortOrder: Int? = null
)

data class SubtaskResponse(
    val id: Long,
    val title: String,
    val completed: Boolean,
    val sortOrder: Int,
    val createdAt: LocalDateTime
)
