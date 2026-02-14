package com.kompralo.services

import com.kompralo.dto.NotificationPageResponse
import com.kompralo.dto.NotificationResponse
import com.kompralo.dto.UnreadCountResponse
import com.kompralo.model.*
import com.kompralo.repository.NotificationRepository
import com.kompralo.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val socketIOService: SocketIOService
) {

    @Transactional
    fun createAndSend(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String,
        priority: String = "medium",
        actionUrl: String? = null,
        relatedEntityId: Long? = null,
        relatedEntityType: RelatedEntityType? = null
    ): NotificationResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

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

        socketIOService.sendToUser(userId, "notification", response)

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
            ?: throw RuntimeException("Notificacion no encontrada")
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
            ?: throw RuntimeException("Notificacion no encontrada")
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
