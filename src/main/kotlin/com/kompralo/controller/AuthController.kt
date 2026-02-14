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

/**
 * Controlador REST para manejar la autenticación de usuarios
 *
 * @RestController: Indica que esta clase es un controlador REST que devuelve JSON
 * @RequestMapping("/api/auth")  Define la ruta base para todos los endpoints (http://localhost:8080/auth)
 * @CrossOrigin: Permite peticiones desde el frontend (React) que corre en otro puerto
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true") // Permite peticiones desde el frontend React
class AuthController(
    // Inyección de dependencias: Spring Boot automáticamente inyecta el AuthService
    private val authService: AuthService
) {

    /**
     * Crea una cookie HTTP-only con el token JWT
     * La cookie persiste por 24 horas (igual que el token JWT)
     */
    private fun createAuthCookie(token: String): Cookie {
        return Cookie("authToken", token).apply {
            isHttpOnly = true  // Protege contra XSS
            secure = false     // true en producción con HTTPS
            path = "/"
            maxAge = 86400     // 24 horas en segundos (igual que jwt.expiration)
            setAttribute("SameSite", "Lax")  // Permite cookies en navegación normal
        }
    }

    /**
     * Crea una cookie vacía para eliminar la cookie de autenticación
     */
    private fun createLogoutCookie(): Cookie {
        return Cookie("authToken", "").apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = 0  // Elimina la cookie inmediatamente
            setAttribute("SameSite", "Lax")
        }
    }

    /**
     * Endpoint para registrar un nuevo usuario
     *
     * Ruta: POST http://localhost:8080/auth/register
     *
     * @param request: Datos del usuario a registrar (nombre, email, password, confirmPassword)
     * @param servletResponse: Response HTTP para agregar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Si el registro es exitoso, devuelve los datos del usuario (token en cookie HTTP-only)
     *         - 400 BAD_REQUEST: Si hay errores de validación (ej: email ya existe, contraseñas no coinciden)
     *         - 500 INTERNAL_SERVER_ERROR: Si ocurre un error inesperado en el servidor
     */
    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.register(request)

            // Guarda el token JWT en cookie HTTP-only
            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

            // Retorna respuesta sin token en el body (va en cookie)
            ResponseEntity.ok(response.copy(token = null))

        } catch (e: IllegalArgumentException) {
            // Captura errores de validación (ej: email duplicado, contraseñas no coinciden)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error en el registro")))

        } catch (e: Exception) {
            // Captura cualquier otro error inesperado
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Endpoint para iniciar sesión
     *
     * Ruta: POST http://localhost:8080/auth/login
     *
     * @param request: Credenciales del usuario (email y password)
     * @param servletResponse: Response HTTP para agregar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Si las credenciales son correctas, devuelve datos del usuario (token en cookie HTTP-only)
     *         - 401 UNAUTHORIZED: Si las credenciales son incorrectas
     *         - 500 INTERNAL_SERVER_ERROR: Si ocurre un error inesperado en el servidor
     */
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.login(request)

            // Guarda el token JWT en cookie HTTP-only
            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

            // Retorna respuesta sin token en el body
            ResponseEntity.ok(response.copy(token = null))

        } catch (e: IllegalArgumentException) {
            // Captura errores de autenticación (email o contraseña incorrectos)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Credenciales inválidas")))

        } catch (e: Exception) {
            // Captura cualquier otro error inesperado
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Endpoint para iniciar sesión con código 2FA
     *
     * Ruta: POST http://localhost:8080/auth/login/2fa
     *
     * @param request: Credenciales del usuario y código 2FA (email, password, twoFactorCode)
     * @param servletResponse: Response HTTP para agregar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Si las credenciales y código 2FA son correctos, devuelve datos del usuario (token en cookie HTTP-only)
     *         - 401 UNAUTHORIZED: Si las credenciales o el código 2FA son incorrectos
     *         - 500 INTERNAL_SERVER_ERROR: Si ocurre un error inesperado en el servidor
     */
    @PostMapping("/login/2fa")
    fun loginWith2FA(
        @RequestBody request: LoginWith2FARequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = authService.loginWith2FA(request)

            // Guarda el token JWT en cookie HTTP-only
            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

            // Retorna respuesta sin token en el body
            ResponseEntity.ok(response.copy(token = null))

        } catch (e: IllegalArgumentException) {
            // Captura errores de autenticación (credenciales o código 2FA incorrectos)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to (e.message ?: "Credenciales o código 2FA inválidos")))

        } catch (e: Exception) {
            // Captura cualquier otro error inesperado
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Endpoint para registrar un nuevo usuario con Google OAuth
     *
     * Ruta: POST http://localhost:8080/api/auth/google/register
     *
     * @param request: {
     *   credential: "token_de_google",
     *   accountType: "user" (Comprador) | "business" (Vendedor/Comercio)
     * }
     * @param servletResponse: Response HTTP para agregar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Si el registro es exitoso, devuelve datos del usuario con el rol seleccionado (token en cookie HTTP-only)
     *         - 400 BAD_REQUEST: Si el email ya existe o accountType es inválido
     *         - 401 UNAUTHORIZED: Si el token de Google es inválido
     */
    @PostMapping("/google/register")
    fun googleRegister(
        @RequestBody request: GoogleRegisterRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            val response = authService.googleRegister(request)

            // Guarda el token JWT en cookie HTTP-only
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

    /**
     * Endpoint para registrar un nuevo usuario con Google OAuth usando access_token (alternativo)
     *
     * Ruta: POST http://localhost:8080/api/auth/google/register-with-token
     *
     * @param request: {
     *   accessToken: "access_token_de_google",
     *   email: "user@gmail.com",
     *   name: "User Name",
     *   googleId: "google_user_id",
     *   accountType: "user" (Comprador) | "business" (Vendedor)
     * }
     * @param servletResponse: Response HTTP para agregar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Si el registro es exitoso
     *         - 400 BAD_REQUEST: Si el email ya existe o datos inválidos
     */
    @PostMapping("/google/register-with-token")
    fun googleRegisterWithToken(
        @RequestBody request: GoogleRegisterWithTokenRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        println("DEBUG Controller: Recibido request - email: ${request.email}, name: ${request.name}, accountType: ${request.accountType}")
        return try {
            val response = authService.googleRegisterWithToken(request)

            // Guarda el token JWT en cookie HTTP-only
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

    /**
     * Endpoint para login con Google OAuth (solo para usuarios existentes)
     *
     * Ruta: POST http://localhost:8080/api/auth/google
     *
     * @param request: {credential: "token_de_google"}
     * @param servletResponse: Response HTTP para agregar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Si el login es exitoso, devuelve datos del usuario (token en cookie HTTP-only)
     *         - 400 BAD_REQUEST: Si el usuario no existe
     *         - 401 UNAUTHORIZED: Si el token de Google es inválido
     */
    @PostMapping("/google")
    fun googleLogin(
        @RequestBody request: GoogleLoginRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<Any> {
        return try {
            val response = authService.googleLogin(request.credential)

            // Guarda el token JWT en cookie HTTP-only
            response.token?.let { servletResponse.addCookie(createAuthCookie(it)) }

            ResponseEntity.ok(response.copy(token = null))
        } catch (e: IllegalArgumentException) {
            // Si el mensaje indica que el usuario no existe, devolver 404
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

    /**
     * Endpoint para cerrar sesión
     *
     * Ruta: POST http://localhost:8080/api/auth/logout
     *
     * @param servletResponse: Response HTTP para eliminar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Sesión cerrada exitosamente
     */
    @PostMapping("/logout")
    fun logout(servletResponse: HttpServletResponse): ResponseEntity<*> {
        // Elimina la cookie de autenticación
        servletResponse.addCookie(createLogoutCookie())

        return ResponseEntity.ok(mapOf("message" to "Sesión cerrada exitosamente"))
    }

    /**
     * Endpoint para eliminar (desactivar) la cuenta del usuario actual
     *
     * Ruta: DELETE http://localhost:8080/api/auth/delete-account
     *
     * @param request: Request HTTP para extraer el token de la cookie
     * @param servletResponse: Response HTTP para eliminar la cookie
     * @return ResponseEntity con:
     *         - 200 OK: Cuenta eliminada exitosamente
     *         - 401 UNAUTHORIZED: Token inválido o expirado
     */
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

            // Eliminar cookie de autenticación
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

    /**
     * Endpoint para obtener información del usuario actual (para debugging)
     *
     * Ruta: GET http://localhost:8080/api/auth/me
     *
     * @param request: Request HTTP para extraer el token de la cookie
     * @return ResponseEntity con información del usuario actual
     */
    @GetMapping("/me")
    fun getCurrentUser(request: jakarta.servlet.http.HttpServletRequest): ResponseEntity<*> {
        return try {
            // Extraer token de la cookie
            val token = request.cookies?.find { it.name == "authToken" }?.value
                ?: throw IllegalArgumentException("No se encontró token de autenticación")

            // Extraer email del token
            val email = authService.extractEmailFromToken(token)

            // Obtener usuario de la base de datos
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