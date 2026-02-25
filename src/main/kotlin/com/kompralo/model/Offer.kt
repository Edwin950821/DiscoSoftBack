package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class OfferType {
    PERCENTAGE,
    FIXED_AMOUNT,
    BUY_X_GET_Y,
    FREE_SHIPPING
}

enum class OfferStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    EXPIRED,
    CANCELLED
}

enum class OfferScope {
    PRODUCT,
    CATEGORY,
    STORE,
    GLOBAL
}

@Entity
@Table(name = "offers")
data class Offer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = true)
    val seller: User? = null,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: OfferType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var scope: OfferScope,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: OfferStatus = OfferStatus.DRAFT,

    @Column(nullable = false, precision = 15, scale = 2)
    var discountValue: BigDecimal,

    @Column(precision = 15, scale = 2)
    var minPurchaseAmount: BigDecimal? = null,

    @Column(precision = 15, scale = 2)
    var maxDiscountAmount: BigDecimal? = null,

    var buyQuantity: Int? = null,

    var getQuantity: Int? = null,

    @Column(columnDefinition = "TEXT")
    var productIds: String? = null,

    @Column(columnDefinition = "TEXT")
    var categoryNames: String? = null,

    @Column(nullable = false)
    var startDate: LocalDateTime,

    @Column(nullable = false)
    var endDate: LocalDateTime,

    var maxUses: Int? = null,

    @Column(nullable = false)
    var currentUses: Int = 0,

    var maxUsesPerUser: Int? = null,

    @Column(length = 500)
    var imageUrl: String? = null,

    @Column(length = 50)
    var badgeText: String? = null,

    var specialDayId: Long? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun isActive(): Boolean = status == OfferStatus.ACTIVE

    fun hasUsesRemaining(): Boolean {
        return maxUses == null || currentUses < maxUses!!
    }

    fun getProductIdList(): List<Long> {
        return productIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
    }

    fun getCategoryNameList(): List<String> {
        return categoryNames?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
}
