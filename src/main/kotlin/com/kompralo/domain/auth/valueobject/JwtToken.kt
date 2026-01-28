package com.kompralo.domain.auth.valueobject

data class JwtToken(val value: String) {
    init {
        require(value.isNotBlank()) { "Token no puede estar vacío" }
    }

    override fun toString(): String = value
}
