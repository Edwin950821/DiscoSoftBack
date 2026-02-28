package com.kompralo.controller

import com.kompralo.dto.PasswordResetConfirm
import com.kompralo.dto.PasswordResetRequest
import com.kompralo.services.PasswordResetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth/password-reset")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class PasswordResetController(
    private val passwordResetService: PasswordResetService
) {

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

    @PostMapping("/confirm")
    fun confirmPasswordReset(@Valid @RequestBody request: PasswordResetConfirm): ResponseEntity<*> {
        return try {
            if (!request.passwordsMatch()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("message" to "Las contraseñas no coinciden"))
            }

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
