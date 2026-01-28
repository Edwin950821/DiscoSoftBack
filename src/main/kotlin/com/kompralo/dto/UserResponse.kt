package com.kompralo.dto

import com.kompralo.model.Role

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: Role,
    val createdAt: String? = null
)