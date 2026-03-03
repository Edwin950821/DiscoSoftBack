package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}

enum class PaymentMethod {
    CASH_ON_DELIVERY,
    CREDIT_CARD,
    DEBIT_CARD,
    TRANSFER,
    PSE,
    PAYPAL,
    WOMPI
}
@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    val buyer: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Column(unique = true, nullable = false, length = 50)
    val orderNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = false, precision = 15, scale = 2)
    val subtotal: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    val discount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    val shipping: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    val tax: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    val total: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    var paymentMethod: PaymentMethod? = null,

    @Column(length = 50)
    var paymentStatus: String = "PENDING",

    var paidAt: LocalDateTime? = null,

    @Column(length = 100)
    var wompiTransactionId: String? = null,

    @Column(columnDefinition = "TEXT")
    val shippingAddress: String,

    @Column(length = 100)
    val shippingCity: String,

    @Column(length = 100)
    val shippingState: String,

    @Column(length = 20)
    val shippingPostalCode: String,

    @Column(length = 100)
    val shippingCountry: String = "Colombia",

    @Column(length = 20)
    val shippingPhone: String,

    @Column(length = 100)
    var trackingNumber: String? = null,

    @Column(length = 100)
    var carrier: String? = null,

    var shippedAt: LocalDateTime? = null,

    var deliveredAt: LocalDateTime? = null,

    var estimatedDeliveryDate: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var buyerNotes: String? = null,

    @Column(columnDefinition = "TEXT")
    var sellerNotes: String? = null,

    var cancelledAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var cancellationReason: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val items: MutableList<OrderItem> = mutableListOf()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun updateStatus(newStatus: OrderStatus) {
        status = newStatus

        when (newStatus) {
            OrderStatus.SHIPPED -> {
                shippedAt = LocalDateTime.now()
                if (estimatedDeliveryDate == null) {
                    estimatedDeliveryDate = LocalDateTime.now().plusDays(5)
                }
            }
            OrderStatus.DELIVERED -> deliveredAt = LocalDateTime.now()
            OrderStatus.CANCELLED -> cancelledAt = LocalDateTime.now()
            else -> {}
        }
    }

    fun markAsPaid(method: PaymentMethod) {
        paymentMethod = method
        paymentStatus = "PAID"
        paidAt = LocalDateTime.now()
    }

    fun canBeCancelled(): Boolean {
        return status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING)
    }

    fun isActive(): Boolean {
        return status !in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)
    }
}
