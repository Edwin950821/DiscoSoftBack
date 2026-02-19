package com.kompralo.services

import com.kompralo.model.AnalyticsEvent
import com.kompralo.repository.AnalyticsEventRepository
import org.springframework.stereotype.Service

@Service
class AnalyticsService(
    private val analyticsEventRepository: AnalyticsEventRepository,
) {

    companion object {
        val ALLOWED_EVENT_TYPES = setOf("PAGE_VIEW", "ADD_TO_CART")
    }

    fun trackBatch(events: List<TrackEventRequest>) {
        val validEvents = events
            .filter { it.eventType in ALLOWED_EVENT_TYPES && it.sellerId > 0 && it.sessionId.isNotBlank() }
            .map { req ->
                AnalyticsEvent(
                    eventType = req.eventType,
                    sellerId = req.sellerId,
                    sessionId = req.sessionId.take(100),
                    productId = req.productId,
                    metadata = req.metadata?.take(500),
                )
            }

        if (validEvents.isNotEmpty()) {
            analyticsEventRepository.saveAll(validEvents)
        }
    }
}

data class TrackEventRequest(
    val eventType: String,
    val sellerId: Long,
    val sessionId: String,
    val productId: Long? = null,
    val metadata: String? = null,
)

data class TrackBatchRequest(
    val events: List<TrackEventRequest>,
)
