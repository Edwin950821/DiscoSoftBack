package com.kompralo.domain.auth.valueobject

data class Password(
    val value: String,
    val isHashed: Boolean = false
) {
    init {
        if (!isHashed) {
            require(value.length >= 6) { "Password debe tener al menos 6 caracteres" }
        }
    }

    companion object {
        fun fromHashed(hashedValue: String): Password {
            return Password(hashedValue, isHashed = true)
        }
    }
}
