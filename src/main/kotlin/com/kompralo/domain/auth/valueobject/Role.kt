package com.kompralo.domain.auth.valueobject

enum class Role {
    USER,      // Comprador
    BUSINESS,  // Vendedor
    ADMIN;     // Administrador

    companion object {
        fun fromString(role: String): Role {
            return when (role.uppercase()) {
                "USER" -> USER
                "BUSINESS" -> BUSINESS
                "ADMIN" -> ADMIN
                else -> throw IllegalArgumentException("Rol inválido: $role")
            }
        }
    }
}
