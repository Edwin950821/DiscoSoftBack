package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class ReturnStatus {
    PENDING,
    IN_REVIEW,
    APPROVED,
    REFUND_ISSUED,
    REJECTED,
    ESCALATED,
    COMPLETED
}

enum class ReturnReason {
    DAMAGED,
    NOT_AS_EXPECTED,
    WRONG_SIZE,
    INCOMPLETE,
    OTHER
}

enum class RequestedSolution {
    EXCHANGE,
    REFUND
}

@Entity
@Table(name = "return_requests")
data class ReturnRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    val buyer: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val reason: ReturnReason,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @ElementCollection
    @CollectionTable(name = "return_request_images", joinColumns = [JoinColumn(name = "return_request_id")])
    @Column(name = "image_url", length = 500)
    val imageUrls: MutableList<String> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    var requestedSolution: RequestedSolution? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ReturnStatus = ReturnStatus.PENDING,

    @Column(columnDefinition = "TEXT")
    var storeResponse: String? = null,

    @Column(columnDefinition = "TEXT")
    var adminNotes: String? = null,

    var requiresProductReturn: Boolean = false,

    @Column(precision = 15, scale = 2)
    var refundAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    var resolvedAt: LocalDateTime? = null,

    var escalatedAt: LocalDateTime? = null,

    var refundIssuedAt: LocalDateTime? = null,

    var refundConfirmedAt: LocalDateTime? = null,

    @Column(length = 50)
    var refundMethod: String? = null
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
