package com.kompralo.controller

import com.kompralo.repository.UserRepository
import com.kompralo.services.NotificationService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
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
        val user = getUser(authentication)
        return ResponseEntity.ok(notificationService.getNotifications(user, page, size))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(authentication: Authentication): ResponseEntity<*> {
        val user = getUser(authentication)
        return ResponseEntity.ok(notificationService.getUnreadCount(user))
    }

    @PutMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val user = getUser(authentication)
        return ResponseEntity.ok(notificationService.markAsRead(id, user))
    }

    @PutMapping("/read-all")
    fun markAllAsRead(authentication: Authentication): ResponseEntity<*> {
        val user = getUser(authentication)
        val count = notificationService.markAllAsRead(user)
        return ResponseEntity.ok(mapOf("markedAsRead" to count))
    }

    @DeleteMapping("/{id}")
    fun deleteNotification(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val user = getUser(authentication)
        notificationService.deleteNotification(id, user)
        return ResponseEntity.noContent().build<Void>()
    }
}
