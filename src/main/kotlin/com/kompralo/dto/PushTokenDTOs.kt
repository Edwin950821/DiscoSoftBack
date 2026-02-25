package com.kompralo.dto

data class RegisterPushTokenRequest(
    val token: String,
    val deviceType: String = "web"
)

data class PushNotificationPayload(
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val actionUrl: String? = null,
    val notificationType: String,
    val data: Map<String, String> = emptyMap()
)
