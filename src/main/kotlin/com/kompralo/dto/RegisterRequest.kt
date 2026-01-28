package com.kompralo.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val accountType: String
)