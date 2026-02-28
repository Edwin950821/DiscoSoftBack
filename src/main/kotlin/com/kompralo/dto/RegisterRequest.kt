package com.kompralo.dto

data class RegisterRequest(
    val username: String,
    val password: String,
    val company_name: String,
    val company_email: String? = null,
    val company_phone: String? = null,
    val company_nit: String? = null,
    val owner_name: String? = null,
    val owner_lastname: String? = null,
    val owner_email: String? = null,
    val plan: String? = "FREE_TRIAL"
)
