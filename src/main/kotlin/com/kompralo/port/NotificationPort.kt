package com.kompralo.port

import com.kompralo.dto.NotificationResponse
import com.kompralo.model.NotificationType
import com.kompralo.model.RelatedEntityType

interface NotificationPort {

    fun createAndSend(
        userId: Long,
        type: NotificationType,
        title: String,
        message: String,
        priority: String = "medium",
        actionUrl: String? = null,
        relatedEntityId: Long? = null,
        relatedEntityType: RelatedEntityType? = null
    ): NotificationResponse
}
