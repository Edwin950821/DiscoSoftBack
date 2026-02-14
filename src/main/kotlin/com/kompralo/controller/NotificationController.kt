package com.kompralo.controller

import com.kompralo.repository.UserRepository
import com.kompralo.services.NotificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class NotificationController(
    private val notificationService: NotificationService,
    private val userRepository: UserRepository
) {

    private fun getUser(authentication: Authentication) =
        userRepository.findByEmail(authentication.name)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

    @GetMapping
    fun getNotifications(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<*> {
        return try {
            val user = getUser(authentication)
            ResponseEntity.ok(notificationService.getNotifications(user, page, size))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener notificaciones")))
        }
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(authentication: Authentication): ResponseEntity<*> {
        return try {
            val user = getUser(authentication)
            ResponseEntity.ok(notificationService.getUnreadCount(user))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener conteo")))
        }
    }

    @PutMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val user = getUser(authentication)
            ResponseEntity.ok(notificationService.markAsRead(id, user))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Notificacion no encontrada")))
        }
    }

    @PutMapping("/read-all")
    fun markAllAsRead(authentication: Authentication): ResponseEntity<*> {
        return try {
            val user = getUser(authentication)
            val count = notificationService.markAllAsRead(user)
            ResponseEntity.ok(mapOf("markedAsRead" to count))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al marcar notificaciones")))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteNotification(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val user = getUser(authentication)
            notificationService.deleteNotification(id, user)
            ResponseEntity.noContent().build<Void>()
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Notificacion no encontrada")))
        }
    }
}
