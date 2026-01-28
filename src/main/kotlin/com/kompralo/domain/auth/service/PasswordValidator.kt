package com.kompralo.domain.auth.service

import com.kompralo.domain.auth.valueobject.Password
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class PasswordValidator(
    private val passwordEncoder: PasswordEncoder
) {
    fun hash(password: Password): Password {
        require(!password.isHashed) { "Password ya está hasheado" }
        val hashedValue = passwordEncoder.encode(password.value)
        return Password.fromHashed(hashedValue)
    }

    fun matches(rawPassword: Password, hashedPassword: Password): Boolean {
        require(!rawPassword.isHashed) { "Password debe ser plano para comparar" }
        require(hashedPassword.isHashed) { "Password hasheado requerido para comparar" }
        return passwordEncoder.matches(rawPassword.value, hashedPassword.value)
    }
}
