package com.kompralo.config

import com.kompralo.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TenantContext(
    private val userRepository: UserRepository
) {

    fun getNegocioId(): UUID {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No hay sesión autenticada")

        val email = auth.name
        val user = userRepository.findByEmail(email)
            .or { userRepository.findByUsername(email) }
            .orElseThrow { IllegalStateException("Usuario no encontrado: $email") }

        return user.negocioId
            ?: throw IllegalStateException("El usuario $email no tiene negocio asignado")
    }
}
