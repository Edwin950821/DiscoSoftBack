package com.kompralo.controller

import com.kompralo.dto.RegisterPushTokenRequest
import com.kompralo.repository.UserRepository
import com.kompralo.services.PushNotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/push")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class PushTokenController(
    private val pushNotificationService: PushNotificationService,
    private val userRepository: UserRepository
) {

    private fun getUser(authentication: Authentication) =
        userRepository.findByEmail(authentication.name)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

    @PostMapping("/token")
    fun registerToken(
        @RequestBody request: RegisterPushTokenRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val user = getUser(authentication)
            val pushToken = pushNotificationService.registerToken(
                user, request.token, request.deviceType
            )
            ResponseEntity.ok(mapOf(
                "message" to "Token registrado exitosamente",
                "id" to pushToken.id
            ))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al registrar token")))
        }
    }

    @DeleteMapping("/token")
    fun removeToken(
        @RequestParam token: String,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            pushNotificationService.removeToken(token)
            ResponseEntity.noContent().build<Void>()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al eliminar token")))
        }
    }
}
