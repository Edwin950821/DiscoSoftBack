package com.kompralo.dto

data class CreateLabelRequest(
    val name: String,
    val color: String = "#6366f1"
)

data class LabelResponse(
    val id: Long,
    val name: String,
    val color: String
)
