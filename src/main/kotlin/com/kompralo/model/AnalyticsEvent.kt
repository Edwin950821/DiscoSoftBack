package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "analytics_events",
    indexes = [
        Index(name = "idx_analytics_seller_type", columnList = "sellerId,eventType"),
        Index(name = "idx_analytics_created", columnList = "createdAt"),
    ]
)
data class AnalyticsEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    val eventType: String,

    @Column(nullable = false)
    val sellerId: Long,

    @Column(nullable = false, length = 100)
    val sessionId: String,

    val productId: Long? = null,

    @Column(columnDefinition = "TEXT")
    val metadata: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
