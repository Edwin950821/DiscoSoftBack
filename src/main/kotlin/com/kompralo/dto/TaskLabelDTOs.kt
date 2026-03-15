package com.kompralo.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateLabelRequest(
    @field:NotBlank(message = "El nombre es requerido")
    @field:Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    val name: String,
    @field:Size(max = 7, message = "El color debe ser un codigo hex valido")
    val color: String = "#6366f1"
)

data class LabelResponse(
    val id: Long,
    val name: String,
    val color: String
)
