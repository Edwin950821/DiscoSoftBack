package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.NotificationPageResponse
import com.kompralo.dto.NotificationResponse
import com.kompralo.dto.PushNotificationPayload
import com.kompralo.dto.UnreadCountResponse
import com.kompralo.model.*
import com.kompralo.repository.NotificationRepository
import com.kompralo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import com.kompralo.port.NotificationPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val socketIOService: SocketIOService,
    private val pushNotificationService: PushNotificationService
) : NotificationPort {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional
    override fun createAndSend(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String,
        priority: String,
        actionUrl: String?,
        relatedEntityId: Long?,
        relatedEntityType: RelatedEntityType?
    ): NotificationResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("Usuario", userId) }

        val notification = Notification(
            user = user,
            type = type,
            title = title,
            message = message,
            priority = priority,
            actionUrl = actionUrl,
            relatedEntityId = relatedEntityId,
            relatedEntityType = relatedEntityType
        )

        val saved = notificationRepository.save(notification)
        val response = saved.toResponse()

        // Socket.IO: entrega en tiempo real (usuario con app abierta)
        socketIOService.sendToUser(userId, "notification", response)

        // FCM: push notification (usuario con app cerrada/background)
        try {
            pushNotificationService.sendToUser(
                user,
                PushNotificationPayload(
                    title = title,
                    body = message,
                    actionUrl = actionUrl,
                    notificationType = type.name,
                    data = buildMap {
                        put("notificationId", saved.id.toString())
                        put("type", type.name)
                        put("priority", priority)
                        relatedEntityId?.let { put("relatedEntityId", it.toString()) }
                        relatedEntityType?.let { put("relatedEntityType", it.name) }
                    }
                )
            )
        } catch (e: Exception) {
            logger.warn("FCM push failed for user $userId: ${e.message}")
        }

        return response
    }

    fun getNotifications(user: User, page: Int, size: Int): NotificationPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pageResult = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
        return NotificationPageResponse(
            notifications = pageResult.content.map { it.toResponse() },
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
            currentPage = page
        )
    }

    fun getUnreadCount(user: User): UnreadCountResponse {
        return UnreadCountResponse(count = notificationRepository.countByUserAndIsReadFalse(user))
    }

    @Transactional
    fun markAsRead(notificationId: Long, user: User): NotificationResponse {
        val notification = notificationRepository.findByIdAndUser(notificationId, user)
            ?: throw ResourceNotFoundException("Notificacion no encontrada")
        notification.isRead = true
        return notificationRepository.save(notification).toResponse()
    }

    @Transactional
    fun markAllAsRead(user: User): Int {
        return notificationRepository.markAllAsReadByUser(user)
    }

    @Transactional
    fun deleteNotification(notificationId: Long, user: User) {
        val notification = notificationRepository.findByIdAndUser(notificationId, user)
            ?: throw ResourceNotFoundException("Notificacion no encontrada")
        notificationRepository.delete(notification)
    }

    private fun Notification.toResponse() = NotificationResponse(
        id = id!!,
        userId = user.id!!,
        type = type,
        title = title,
        message = message,
        isRead = isRead,
        priority = priority,
        actionUrl = actionUrl,
        relatedEntityId = relatedEntityId,
        relatedEntityType = relatedEntityType,
        createdAt = createdAt
    )
}
