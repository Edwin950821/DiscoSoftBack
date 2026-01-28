package com.kompralo.domain.auth.valueobject

data class TotpSecret(val value: String) {
    init {
        require(value.isNotBlank()) { "Secret TOTP no puede estar vacío" }
        require(value.length >= 16) { "Secret TOTP debe tener al menos 16 caracteres" }
    }

    override fun toString(): String = value
}
