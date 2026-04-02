package com.kompralo.controller

import com.kompralo.dto.DiscoAuthResponse
import com.kompralo.dto.DiscoLoginRequest
import com.kompralo.dto.DiscoRol
import com.kompralo.model.Role
import com.kompralo.repository.DiscoMeseroRepository
import com.kompralo.repository.NegocioRepository
import com.kompralo.repository.UserRepository
import com.kompralo.services.JwtService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/disco/auth")
class DiscoAuthController(
    private val userRepository: UserRepository,
    private val meseroRepo: DiscoMeseroRepository,
    private val negocioRepo: NegocioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    private val log = LoggerFactory.getLogger(DiscoAuthController::class.java)

    private fun discoRolToRole(rol: DiscoRol): Role = when (rol) {
        DiscoRol.ADMINISTRADOR -> Role.ADMIN
        DiscoRol.DUENO -> Role.OWNER
        DiscoRol.MESERO -> Role.MESERO
    }

    private fun roleToDiscoRol(role: Role): DiscoRol = when (role) {
        Role.ADMIN -> DiscoRol.ADMINISTRADOR
        Role.OWNER -> DiscoRol.DUENO
        Role.MESERO -> DiscoRol.MESERO
        else -> throw IllegalArgumentException("Rol no autorizado para Monastery Club")
    }

    private fun isSecureRequest(request: HttpServletRequest): Boolean {
        return request.isSecure ||
            request.getHeader("X-Forwarded-Proto") == "https" ||
            request.getHeader("Origin")?.startsWith("https://") == true
    }

    private fun createAuthCookie(token: String, isSecure: Boolean): Cookie {
        return Cookie("authToken", token).apply {
            isHttpOnly = true
            secure = isSecure
            path = "/"
            maxAge = 86400
            setAttribute("SameSite", if (isSecure) "None" else "Lax")
        }
    }

    private fun createLogoutCookie(isSecure: Boolean): Cookie {
        return Cookie("authToken", "").apply {
            isHttpOnly = true
            secure = isSecure
            path = "/"
            maxAge = 0
            setAttribute("SameSite", if (isSecure) "None" else "Lax")
        }
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: DiscoLoginRequest,
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val user = userRepository.findByEmail(request.username)
                .or { userRepository.findByUsername(request.username) }
                .orElseThrow { IllegalArgumentException("Credenciales inválidas") }

            if (!passwordEncoder.matches(request.password, user.password)) {
                throw IllegalArgumentException("Credenciales inválidas")
            }

            if (user.isActive != true) {
                throw IllegalArgumentException("Usuario inactivo")
            }

            val allowedRoles = listOf(Role.ADMIN, Role.OWNER, Role.MESERO)
            if (user.role !in allowedRoles) {
                throw IllegalArgumentException("No tienes acceso a Monastery Club")
            }

            val accessToken = jwtService.generateToken(user.email, user.role.name)
            val refreshToken = jwtService.generateToken(user.email, "refresh")
            val secure = isSecureRequest(servletRequest)
            servletResponse.addCookie(createAuthCookie(accessToken, secure))

            val negocioId = user.negocioId
            val negocio = negocioId?.let { negocioRepo.findById(it).orElse(null) }

            val meseroId = if (user.role == Role.MESERO && !user.username.isNullOrBlank()) {
                if (negocioId != null) {
                    meseroRepo.findByNegocioIdAndUsername(negocioId, user.username!!)?.id?.toString()
                } else {
                    meseroRepo.findByUsername(user.username!!)?.id?.toString()
                }
            } else null

            ResponseEntity.ok(DiscoAuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                nombre = user.name,
                rol = roleToDiscoRol(user.role),
                meseroId = meseroId,
                negocioId = negocioId?.toString(),
                negocioNombre = negocio?.nombre,
                mensaje = "Bienvenido a Monastery Club"
            ))

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Credenciales inválidas")))
        } catch (e: Exception) {
            log.error("Error en disco login: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @PostMapping("/logout")
    fun logout(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        servletResponse.addCookie(createLogoutCookie(isSecureRequest(servletRequest)))
        return ResponseEntity.ok(mapOf("message" to "Sesión cerrada exitosamente"))
    }
}
