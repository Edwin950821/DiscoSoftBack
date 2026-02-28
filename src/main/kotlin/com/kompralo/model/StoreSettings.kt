package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "store_settings")
data class StoreSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", unique = true, nullable = false, foreignKey = ForeignKey(name = "fk_store_settings_seller"))
    val seller: User,

    @Column(length = 10, nullable = false)
    var currency: String = "COP",

    @Column(nullable = false)
    var maintenanceMode: Boolean = false,

    @Column(columnDefinition = "TEXT", nullable = false)
    var enabledPaymentMethods: String = "CASH_ON_DELIVERY,TRANSFER",

    @Column(length = 50, nullable = false)
    var defaultPaymentMethod: String = "CASH_ON_DELIVERY",

    @Column(nullable = false)
    var taxIncludedInPrice: Boolean = false,

    @Column(nullable = false)
    var autoTaxUpdate: Boolean = false,

    @Column(nullable = false)
    var notifyNewOrder: Boolean = true,

    @Column(nullable = false)
    var notifyOrderStatusChange: Boolean = true,

    @Column(nullable = false)
    var notifyLowStock: Boolean = true,

    @Column(nullable = false)
    var notifyNewCustomer: Boolean = false,

    @Column(nullable = false)
    var emailNotifications: Boolean = true,

    @Column(nullable = false)
    var pushNotifications: Boolean = true,

    @Column(length = 20, nullable = false)
    var primaryColor: String = "#059669",

    @Column(length = 500)
    var bannerUrl: String? = null,

    @Column(length = 255)
    var facebookUrl: String? = null,

    @Column(length = 255)
    var instagramUrl: String? = null,

    @Column(length = 255)
    var twitterUrl: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun getEnabledMethodsList(): List<String> {
        return enabledPaymentMethods.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun setEnabledMethodsList(methods: List<String>) {
        enabledPaymentMethods = methods.joinToString(",")
    }
}
