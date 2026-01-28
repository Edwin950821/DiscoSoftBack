package com.kompralo.domain.auth.valueobject

data class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email no puede estar vacío" }
        require(value.matches(EMAIL_REGEX)) { "Email inválido: $value" }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    }

    override fun toString(): String = value
}
