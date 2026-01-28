package com.kompralo.dto

data class ErrorResponse(
    val message: String,
    val status: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)