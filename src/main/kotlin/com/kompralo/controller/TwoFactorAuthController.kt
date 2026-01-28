package com.kompralo.controller

import com.kompralo.dto.TwoFactorStatusResponse
import com.kompralo.dto.TwoFactorVerifyRequest
import com.kompralo.repository.UserRepository
import com.kompralo.services.TwoFactorAuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para autenticación de dos factores (2FA)
 *
 * Endpoints:
 * - GET  /api/auth/2fa/status - Ver estado de 2FA
 * - POST /api/auth/2fa/setup - Iniciar setup de 2FA
 * - POST /api/auth/2fa/enable - Habilitar 2FA con código
 * - POST /api/auth/2fa/disable - Deshabilitar 2FA con código
 * - POST /api/auth/2fa/verify - Verificar código 2FA (durante login)
 */
@RestController
@RequestMapping("/api/auth/2fa")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class TwoFactorAuthController(
    private val twoFactorAuthService: TwoFactorAuthService,
    private val userRepository: UserRepository
) {

    /**
     * Obtiene el estado de 2FA del usuario autenticado
     *
     * Ruta: GET /api/auth/2fa/status
     *
     * @param authentication Usuario autenticado
     * @return Estado de 2FA (habilitado/deshabilitado y códigos restantes)
     */
    @GetMapping("/status")
    fun getStatus(authentication: Authentication): ResponseEntity<TwoFactorStatusResponse> {
        val email = authentication.name
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val status = twoFactorAuthService.getTwoFactorStatus(user)

        return ResponseEntity.ok(status)
    }

    /**
     * Inicia el setup de 2FA
     * Genera secreto TOTP, QR code y códigos de respaldo
     *
     * Ruta: POST /api/auth/2fa/setup
     *
     * @param authentication Usuario autenticado
     * @return Secreto, URL de QR code y códigos de respaldo
     */
    @PostMapping("/setup")
    fun setup(authentication: Authentication): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val user = userRepository.findByEmail(email)
                .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

            val setupResponse = twoFactorAuthService.setupTwoFactor(user)

            ResponseEntity.ok(setupResponse)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al configurar 2FA")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Habilita 2FA después de verificar el código
     *
     * Ruta: POST /api/auth/2fa/enable
     *
     * @param request Código TOTP de 6 dígitos
     * @param authentication Usuario autenticado
     * @return 200 OK si se habilitó correctamente
     */
    @PostMapping("/enable")
    fun enable(
        @Valid @RequestBody request: TwoFactorVerifyRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val user = userRepository.findByEmail(email)
                .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

            twoFactorAuthService.enableTwoFactor(user, request.code)

            ResponseEntity.ok(
                mapOf("message" to "2FA habilitado exitosamente")
            )

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al habilitar 2FA")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Deshabilita 2FA con código de verificación
     *
     * Ruta: POST /api/auth/2fa/disable
     *
     * @param request Código TOTP de 6 dígitos o código de respaldo
     * @param authentication Usuario autenticado
     * @return 200 OK si se deshabilitó correctamente
     */
    @PostMapping("/disable")
    fun disable(
        @Valid @RequestBody request: TwoFactorVerifyRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val user = userRepository.findByEmail(email)
                .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

            twoFactorAuthService.disableTwoFactor(user, request.code)

            ResponseEntity.ok(
                mapOf("message" to "2FA deshabilitado exitosamente")
            )

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al deshabilitar 2FA")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Verifica un código 2FA
     * Este endpoint se usará durante el login cuando el usuario tenga 2FA habilitado
     *
     * Ruta: POST /api/auth/2fa/verify
     *
     * @param request Código TOTP de 6 dígitos
     * @param authentication Usuario autenticado
     * @return 200 OK si el código es válido
     */
    @PostMapping("/verify")
    fun verify(
        @Valid @RequestBody request: TwoFactorVerifyRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val user = userRepository.findByEmail(email)
                .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

            val isValid = twoFactorAuthService.verifyTwoFactorCode(user, request.code)

            if (isValid) {
                ResponseEntity.ok(
                    mapOf(
                        "valid" to true,
                        "message" to "Código verificado exitosamente"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                        mapOf(
                            "valid" to false,
                            "message" to "Código inválido"
                        )
                    )
            }

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }
}
