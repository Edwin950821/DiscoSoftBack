package com.kompralo.controller

import com.kompralo.dto.GoogleLoginRequest
import com.kompralo.dto.GoogleRegisterRequest
import com.kompralo.dto.GoogleRegisterWithTokenRequest
import com.kompralo.dto.LoginRequest
import com.kompralo.dto.LoginWith2FARequest
import com.kompralo.dto.RegisterRequest
import com.kompralo.dto.ChangePasswordRequest
import com.kompralo.dto.UpdateProfilePictureRequest
import com.kompralo.services.AuthService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    private val log = org.slf4j.LoggerFactory.getLogger(AuthController::class.java)

    private fun createAuthCookie(token: String, isSecure: Boolean = false): Cookie {
        return Cookie("authToken", token).apply {
            isHttpOnly = true
            secure = isSecure
            path = "/"
            maxAge = 86400
            setAttribute("SameSite", if (isSecure) "None" else "Lax")
        }
    }

    private fun createLogoutCookie(isSecure: Boolean = false): Cookie {
        return Cookie("authToken", "").apply {
            isHttpOnly = true
            secure = isSecure
            path = "/"
            maxAge = 0
            setAttribute("SameSite", if (isSecure) "None" else "Lax")
        }
    }

    private fun isSecureRequest(request: jakarta.servlet.http.HttpServletRequest): Boolean {
        return request.isSecure ||
            request.getHeader("X-Forwarded-Proto") == "https" ||
            request.getHeader("Origin")?.startsWith("https://") == true
    }

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.register(request)
            val secure = isSecureRequest(servletRequest)
            response.token?.let { servletResponse.addCookie(createAuthCookie(it, secure)) }

            ResponseEntity.ok(response)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to (e.message ?: "Error en el registro")))

        } catch (e: Exception) {
            log.error("Error en register: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.login(request)
            val secure = isSecureRequest(servletRequest)
            response.token?.let { servletResponse.addCookie(createAuthCookie(it, secure)) }

            ResponseEntity.ok(response)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Credenciales inválidas")))

        } catch (e: Exception) {
            log.error("Error en login: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @PostMapping("/login/2fa")
    fun loginWith2FA(
        @Valid @RequestBody request: LoginWith2FARequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.loginWith2FA(request)
            val secure = isSecureRequest(servletRequest)
            response.token?.let { servletResponse.addCookie(createAuthCookie(it, secure)) }

            ResponseEntity.ok(response.copy(token = null))

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Credenciales o código 2FA inválidos")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @PostMapping("/google/register")
    fun googleRegister(
        @Valid @RequestBody request: GoogleRegisterRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            val response = authService.googleRegister(request)
            val secure = isSecureRequest(servletRequest)
            response.token?.let { servletResponse.addCookie(createAuthCookie(it, secure)) }

            ResponseEntity.ok(response.copy(token = null))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error en el registro con Google")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Token de Google inválido")))
        }
    }

    @PostMapping("/google/register-with-token")
    fun googleRegisterWithToken(
        @Valid @RequestBody request: GoogleRegisterWithTokenRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            val response = authService.googleRegisterWithToken(request)
            val secure = isSecureRequest(servletRequest)
            response.token?.let { servletResponse.addCookie(createAuthCookie(it, secure)) }

            ResponseEntity.ok(response.copy(token = null))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error en el registro con Google")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error interno del servidor")))
        }
    }

    @PostMapping("/google")
    fun googleLogin(
        @Valid @RequestBody request: GoogleLoginRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            val response = authService.googleLogin(request.credential)
            val secure = isSecureRequest(servletRequest)
            response.token?.let { servletResponse.addCookie(createAuthCookie(it, secure)) }

            ResponseEntity.ok(response.copy(token = null))
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("no encontrado", ignoreCase = true) == true ||
                e.message?.contains("no tienes una cuenta", ignoreCase = true) == true) {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("message" to (e.message ?: "Usuario no encontrado")))
            } else {
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("message" to (e.message ?: "Error en los datos")))
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Error en autenticación con Google")))
        }
    }

    @PostMapping("/logout")
    fun logout(
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        servletResponse.addCookie(createLogoutCookie(isSecureRequest(servletRequest)))

        return ResponseEntity.ok(mapOf("message" to "Sesión cerrada exitosamente"))
    }

    @DeleteMapping("/delete-account")
    fun deleteAccount(
        request: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val token = request.cookies?.find { it.name == "authToken" }?.value
                ?: throw IllegalArgumentException("No se encontró token de autenticación")

            val email = authService.extractEmailFromToken(token)
            authService.deleteAccount(email)

            servletResponse.addCookie(createLogoutCookie(isSecureRequest(request)))

            ResponseEntity.ok(mapOf("message" to "Cuenta eliminada exitosamente"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Token inválido o expirado"))
        }
    }

    @PutMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody body: ChangePasswordRequest,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val token = request.cookies?.find { it.name == "authToken" }?.value
                ?: throw IllegalArgumentException("No se encontro token de autenticacion")

            val email = authService.extractEmailFromToken(token)
            authService.changePassword(email, body.currentPassword, body.newPassword)

            ResponseEntity.ok(mapOf("message" to "Contrasena actualizada exitosamente"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al cambiar contrasena")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Token invalido o expirado"))
        }
    }

    @PutMapping("/profile-picture")
    fun updateProfilePicture(
        @Valid @RequestBody body: UpdateProfilePictureRequest,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val token = request.cookies?.find { it.name == "authToken" }?.value
                ?: throw IllegalArgumentException("No se encontró token de autenticación")

            val email = authService.extractEmailFromToken(token)
            val response = authService.updateProfilePicture(email, body.imageUrl)

            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar foto")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Token inválido o expirado"))
        }
    }

    @GetMapping("/me")
    fun getCurrentUser(request: jakarta.servlet.http.HttpServletRequest): ResponseEntity<*> {
        return try {
            val token = request.cookies?.find { it.name == "authToken" }?.value
                ?: throw IllegalArgumentException("No se encontró token de autenticación")

            val email = authService.extractEmailFromToken(token)

            val user = authService.getUserByEmail(email)

            ResponseEntity.ok(mapOf(
                "user" to user,
                "message" to "Usuario autenticado correctamente"
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Token inválido o expirado"))
        }
    }
}
