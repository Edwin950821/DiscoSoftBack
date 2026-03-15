package com.kompralo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateSubtaskRequest(
    @field:NotBlank(message = "El titulo es requerido")
    @field:Size(max = 255, message = "El titulo no puede exceder 255 caracteres")
    val title: String
)

data class UpdateSubtaskRequest(
    @field:Size(max = 255, message = "El titulo no puede exceder 255 caracteres")
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
