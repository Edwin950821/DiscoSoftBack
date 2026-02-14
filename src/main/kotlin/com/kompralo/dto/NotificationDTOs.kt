package com.kompralo.dto

import com.kompralo.model.NotificationType
import com.kompralo.model.RelatedEntityType
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val priority: String,
    val actionUrl: String?,
    val relatedEntityId: Long?,
    val relatedEntityType: RelatedEntityType?,
    val createdAt: LocalDateTime
)

data class UnreadCountResponse(
    val count: Long
)

data class NotificationPageResponse(
    val notifications: List<NotificationResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)
