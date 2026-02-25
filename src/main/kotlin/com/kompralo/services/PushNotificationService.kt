package com.kompralo.services

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.*
import com.kompralo.dto.PushNotificationPayload
import com.kompralo.model.PushToken
import com.kompralo.model.User
import com.kompralo.repository.PushTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PushNotificationService(
    private val pushTokenRepository: PushTokenRepository
) {
    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    private fun isFirebaseAvailable(): Boolean = FirebaseApp.getApps().isNotEmpty()

    @Transactional
    fun registerToken(user: User, token: String, deviceType: String): PushToken {
        val existing = pushTokenRepository.findByToken(token)
        if (existing != null) {
            existing.active = true
            existing.lastUsedAt = LocalDateTime.now()
            return pushTokenRepository.save(existing)
        }
        return pushTokenRepository.save(
            PushToken(user = user, token = token, deviceType = deviceType)
        )
    }

    @Transactional
    fun removeToken(token: String) {
        pushTokenRepository.deleteByToken(token)
    }

    fun sendToUser(user: User, payload: PushNotificationPayload) {
        if (!isFirebaseAvailable()) return

        val tokens = pushTokenRepository.findByUserAndActiveTrue(user)
        if (tokens.isEmpty()) return

        tokens.forEach { pushToken ->
            try {
                val message = buildMessage(pushToken.token, payload)
                FirebaseMessaging.getInstance().send(message)
                pushToken.lastUsedAt = LocalDateTime.now()
                pushTokenRepository.save(pushToken)
            } catch (e: FirebaseMessagingException) {
                handleSendError(e, pushToken)
            } catch (e: Exception) {
                logger.error("Error sending push to ${pushToken.token.take(20)}...: ${e.message}")
            }
        }
    }

    fun sendToAllBuyers(payload: PushNotificationPayload) {
        if (!isFirebaseAvailable()) return

        val allTokens = pushTokenRepository.findAllActive()
        if (allTokens.isEmpty()) return

        allTokens.chunked(500).forEach { chunk ->
            try {
                val multicast = buildMulticastMessage(
                    chunk.map { it.token },
                    payload
                )
                val response = FirebaseMessaging.getInstance().sendEachForMulticast(multicast)
                handleMulticastResponse(response, chunk)
                logger.info("Multicast push sent: ${response.successCount} success, ${response.failureCount} failed")
            } catch (e: Exception) {
                logger.error("Error sending multicast push: ${e.message}")
            }
        }
    }

    fun sendOfferNotification(
        title: String,
        body: String,
        offerId: Long? = null,
        imageUrl: String? = null
    ) {
        val payload = PushNotificationPayload(
            title = title,
            body = body,
            imageUrl = imageUrl,
            actionUrl = offerId?.let { "/ofertas/$it" },
            notificationType = "PROMO_OFFER",
            data = buildMap {
                put("type", "PROMO_OFFER")
                offerId?.let { put("offerId", it.toString()) }
            }
        )
        sendToAllBuyers(payload)
    }

    private fun buildMessage(token: String, payload: PushNotificationPayload): Message {
        val notification = Notification.builder()
            .setTitle(payload.title)
            .setBody(payload.body)
            .apply { payload.imageUrl?.let { setImage(it) } }
            .build()

        val webpushConfig = WebpushConfig.builder()
            .setNotification(
                WebpushNotification.builder()
                    .setIcon("/Kompralo.png")
                    .setBadge("/Kompralo.png")
                    .build()
            )
            .apply {
                payload.actionUrl?.let { url ->
                    setFcmOptions(
                        WebpushFcmOptions.builder().setLink(url).build()
                    )
                }
            }
            .build()

        return Message.builder()
            .setToken(token)
            .setNotification(notification)
            .setWebpushConfig(webpushConfig)
            .putAllData(payload.data)
            .build()
    }

    private fun buildMulticastMessage(
        tokens: List<String>,
        payload: PushNotificationPayload
    ): MulticastMessage {
        val notification = Notification.builder()
            .setTitle(payload.title)
            .setBody(payload.body)
            .apply { payload.imageUrl?.let { setImage(it) } }
            .build()

        return MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(notification)
            .putAllData(payload.data)
            .build()
    }

    @Transactional
    private fun handleSendError(e: FirebaseMessagingException, pushToken: PushToken) {
        when (e.messagingErrorCode) {
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT -> {
                pushToken.active = false
                pushTokenRepository.save(pushToken)
                logger.info("Deactivated invalid push token: ${pushToken.token.take(20)}...")
            }
            else -> {
                logger.error("FCM send error: ${e.messagingErrorCode} - ${e.message}")
            }
        }
    }

    @Transactional
    private fun handleMulticastResponse(response: BatchResponse, tokens: List<PushToken>) {
        response.responses.forEachIndexed { index, sendResponse ->
            if (!sendResponse.isSuccessful) {
                val error = sendResponse.exception
                if (error?.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                    error?.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT
                ) {
                    tokens[index].active = false
                    pushTokenRepository.save(tokens[index])
                }
            }
        }
    }
}
