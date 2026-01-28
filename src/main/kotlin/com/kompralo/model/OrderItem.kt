package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entidad OrderItem - Representa un item/producto dentro de una orden
 *
 * Guarda un snapshot del producto en el momento de la compra para mantener
 * histórico aunque el producto cambie después
 */
@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Relación con la orden
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    // ID del producto (referencia, el producto puede cambiar después)
    @Column(nullable = false)
    val productId: Long,

    // Snapshot del producto en momento de compra
    @Column(nullable = false, length = 255)
    val productName: String,

    @Column(length = 255)
    val productSku: String? = null,

    @Column(length = 500)
    val productImageUrl: String? = null,

    // Variante si aplica
    val variantId: Long? = null,

    @Column(length = 255)
    val variantName: String? = null,

    // Cantidad
    @Column(nullable = false)
    val quantity: Int,

    // Precio unitario en momento de compra (snapshot)
    @Column(nullable = false, precision = 15, scale = 2)
    val unitPrice: BigDecimal,

    // Precio original (antes de descuentos)
    @Column(precision = 15, scale = 2)
    val originalPrice: BigDecimal? = null,

    // Descuento aplicado al item
    @Column(nullable = false, precision = 15, scale = 2)
    val discount: BigDecimal = BigDecimal.ZERO,

    // Subtotal (quantity * unitPrice - discount)
    @Column(nullable = false, precision = 15, scale = 2)
    val subtotal: BigDecimal,

    // Timestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Calcula el total del item
     */
    fun calculateTotal(): BigDecimal {
        return (unitPrice * quantity.toBigDecimal()) - discount
    }
}
