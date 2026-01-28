package com.kompralo.controller

import com.kompralo.dto.PasswordResetConfirm
import com.kompralo.dto.PasswordResetRequest
import com.kompralo.services.PasswordResetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para recuperación de contraseña
 *
 * Endpoints:
 * - POST /api/auth/password-reset/request - Solicitar recuperación
 * - POST /api/auth/password-reset/confirm - Confirmar con token
 * - GET /api/auth/password-reset/validate/{token} - Validar token
 */
@RestController
@RequestMapping("/api/auth/password-reset")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class PasswordResetController(
    private val passwordResetService: PasswordResetService
) {

    /**
     * Solicita recuperación de contraseña
     *
     * Ruta: POST /api/auth/password-reset/request
     *
     * @param request DTO con email del usuario
     * @return 200 OK si se envió el email
     */
    @PostMapping("/request")
    fun requestPasswordReset(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<*> {
        return try {
            val emailSent = passwordResetService.requestPasswordReset(request.email)

            if (emailSent) {
                ResponseEntity.ok(
                    mapOf(
                        "message" to "Si el email existe, recibirás instrucciones para resetear tu contraseña"
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("message" to "Error al enviar el email. Intenta nuevamente"))
            }

        } catch (e: IllegalArgumentException) {
            // Por seguridad, siempre devolver el mismo mensaje
            // No revelar si el usuario existe o no
            ResponseEntity.ok(
                mapOf(
                    "message" to "Si el email existe, recibirás instrucciones para resetear tu contraseña"
                )
            )

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Confirma el reset de contraseña con token
     *
     * Ruta: POST /api/auth/password-reset/confirm
     *
     * @param request DTO con token y nueva contraseña
     * @return 200 OK si se actualizó la contraseña
     */
    @PostMapping("/confirm")
    fun confirmPasswordReset(@Valid @RequestBody request: PasswordResetConfirm): ResponseEntity<*> {
        return try {
            // Validar que las contraseñas coincidan
            if (!request.passwordsMatch()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("message" to "Las contraseñas no coinciden"))
            }

            // Confirmar reset
            passwordResetService.confirmPasswordReset(request.token, request.newPassword)

            ResponseEntity.ok(
                mapOf("message" to "Contraseña actualizada exitosamente. Ya puedes iniciar sesión")
            )

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Token inválido o expirado")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    /**
     * Valida si un token es válido
     *
     * Ruta: GET /api/auth/password-reset/validate/{token}
     *
     * @param token Token a validar
     * @return 200 OK con válido:true/false
     */
    @GetMapping("/validate/{token}")
    fun validateToken(@PathVariable token: String): ResponseEntity<*> {
        val isValid = passwordResetService.validateToken(token)

        return ResponseEntity.ok(
            mapOf(
                "valid" to isValid,
                "message" to if (isValid) "Token válido" else "Token inválido o expirado"
            )
        )
    }
}
