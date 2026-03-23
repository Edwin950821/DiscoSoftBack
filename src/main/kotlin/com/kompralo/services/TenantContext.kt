package com.kompralo.services

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
            ?: throw IllegalStateException("No hay usuario autenticado")
        val email = auth.name
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalStateException("Usuario no encontrado: $email") }
        return user.negocioId
            ?: throw IllegalStateException("El usuario $email no tiene negocio asignado")
    }
}
