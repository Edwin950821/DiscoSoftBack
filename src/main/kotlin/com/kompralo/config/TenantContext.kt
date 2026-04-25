package com.kompralo.config

import com.kompralo.model.Role
import com.kompralo.repository.NegocioRepository
import com.kompralo.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TenantContext(
    private val userRepository: UserRepository,
    private val negocioRepository: NegocioRepository,
    private val request: HttpServletRequest
) {

    fun getNegocioId(): UUID {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No hay sesión autenticada")

        val email = auth.name
        val user = userRepository.findByEmail(email)
            .or { userRepository.findByUsername(email) }
            .orElseThrow { IllegalStateException("Usuario no encontrado: $email") }

        // SUPER: el negocio activo se selecciona vía header X-Negocio-Id
        // (los demás roles ignoran el header para evitar escalada de privilegio).
        if (user.role == Role.SUPER) {
            val raw = request.getHeader("X-Negocio-Id")
                ?: throw IllegalStateException("Falta header X-Negocio-Id (requerido para SUPER)")
            val negocioId = try {
                UUID.fromString(raw.trim())
            } catch (e: IllegalArgumentException) {
                throw IllegalStateException("X-Negocio-Id inválido: $raw")
            }
            if (!negocioRepository.existsById(negocioId)) {
                throw IllegalStateException("Negocio no encontrado: $negocioId")
            }
            return negocioId
        }

        return user.negocioId
            ?: throw IllegalStateException("El usuario $email no tiene negocio asignado")
    }
}
