package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class ClaimType {
    DELAY,
    NOT_RECEIVED,
    WRONG_ADDRESS
}

enum class ClaimStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    CLOSED,
    EXTENDED
}

enum class ClaimResolution {
    REFUND,
    EXTENDED,
    DELIVERED
}

@Entity
@Table(name = "shipping_claims")
data class ShippingClaim(
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
    val type: ClaimType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ClaimStatus = ClaimStatus.OPEN,

    val estimatedDeliveryDate: LocalDateTime? = null,

    @Column(nullable = false)
    val claimDate: LocalDateTime = LocalDateTime.now(),

    @Column(columnDefinition = "TEXT")
    var storeResponse: String? = null,

    var storeResponseDate: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    var resolution: ClaimResolution? = null,

    var resolvedAt: LocalDateTime? = null,

    var autoResolved: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
