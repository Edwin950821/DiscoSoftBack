package com.kompralo.controller

import com.kompralo.dto.GoogleLoginRequest
import com.kompralo.dto.GoogleRegisterRequest
import com.kompralo.dto.GoogleRegisterWithTokenRequest
import com.kompralo.dto.LoginRequest
import com.kompralo.dto.LoginWith2FARequest
import com.kompralo.dto.RegisterRequest
import com.kompralo.services.AuthService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class AuthController(
    private val authService: AuthService
) {

    private fun createAuthCookie(token: String): Cookie {
        return Cookie("authToken", token).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = 86400
            setAttribute("SameSite", "Lax")
        }
    }

    private fun createLogoutCookie(): Cookie {
        return Cookie("authToken", "").apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = 0
            setAttribute("SameSite", "Lax")
        }
    }

    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.register(request)

            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

            ResponseEntity.ok(response)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to (e.message ?: "Error en el registro")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.login(request)

            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

            ResponseEntity.ok(response)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Credenciales inválidas")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @PostMapping("/login/2fa")
    fun loginWith2FA(
        @RequestBody request: LoginWith2FARequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.loginWith2FA(request)

            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

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
        @RequestBody request: GoogleRegisterRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            val response = authService.googleRegister(request)

            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

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
        @RequestBody request: GoogleRegisterWithTokenRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        println("DEBUG Controller: Recibido request - email: ${request.email}, name: ${request.name}, accountType: ${request.accountType}")
        return try {
            val response = authService.googleRegisterWithToken(request)

            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

            ResponseEntity.ok(response.copy(token = null))
        } catch (e: IllegalArgumentException) {
            println("ERROR IllegalArgument: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error en el registro con Google")))
        } catch (e: Exception) {
            println("ERROR Exception: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error interno del servidor")))
        }
    }

    @PostMapping("/google")
    fun googleLogin(
        @RequestBody request: GoogleLoginRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            val response = authService.googleLogin(request.credential)

            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

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
    fun logout(servletResponse: HttpServletResponse): ResponseEntity<*> {
        servletResponse.addCookie(createLogoutCookie())

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

            servletResponse.addCookie(createLogoutCookie())

            ResponseEntity.ok(mapOf("message" to "Cuenta eliminada exitosamente"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to e.message))
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
