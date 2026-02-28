package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false, length = 255)
    val productName: String,

    @Column(length = 255)
    val productSku: String? = null,

    @Column(length = 500)
    val productImageUrl: String? = null,

    val variantId: Long? = null,

    @Column(length = 255)
    val variantName: String? = null,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false, precision = 15, scale = 2)
    val unitPrice: BigDecimal,

    @Column(precision = 15, scale = 2)
    val originalPrice: BigDecimal? = null,

    @Column(nullable = false, precision = 15, scale = 2)
    val discount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 2)
    val subtotal: BigDecimal,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun calculateTotal(): BigDecimal {
        return (unitPrice * quantity.toBigDecimal()) - discount
    }
}
