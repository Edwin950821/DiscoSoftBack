package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class SellerStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    BANNED
}

@Entity
@Table(name = "seller_profiles")
data class SellerProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false, foreignKey = ForeignKey(name = "fk_seller_profile_user"))
    val user: User,

    @Column(nullable = false)
    var businessName: String,

    @Column(length = 100)
    var businessType: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(length = 500)
    var logoUrl: String? = null,

    @Column(length = 20)
    var phone: String? = null,

    @Column(length = 255)
    var website: String? = null,

    @Column(length = 50)
    var taxId: String? = null,

    @Column(columnDefinition = "TEXT")
    var address: String? = null,

    @Column(length = 100)
    var city: String? = null,

    @Column(length = 100)
    var state: String? = null,

    @Column(length = 20)
    var postalCode: String? = null,

    @Column(length = 100)
    var country: String = "Colombia",

    @Column(nullable = false)
    var verified: Boolean = false,

    var verificationDate: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: SellerStatus = SellerStatus.PENDING,

    @Column(precision = 15, scale = 2)
    var totalSales: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var totalProducts: Int = 0,

    @Column(precision = 3, scale = 2)
    var averageRating: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var totalReviews: Int = 0,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun verify() {
        verified = true
        verificationDate = LocalDateTime.now()
        if (status == SellerStatus.PENDING) {
            status = SellerStatus.ACTIVE
        }
    }

    fun canSell(): Boolean {
        return verified && status == SellerStatus.ACTIVE
    }
}
