package com.kompralo.dto

data class ReportProblemRequest(
    val tipo: String,
    val seccion: String,
    val descripcion: String
)

data class ReportProblemResponse(
    val success: Boolean,
    val message: String
)
