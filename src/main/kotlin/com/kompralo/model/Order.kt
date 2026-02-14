package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Estados posibles de una orden
 */
enum class OrderStatus {
    PENDING,       // Pendiente de confirmación
    CONFIRMED,     // Confirmada
    PROCESSING,    // En proceso de preparación
    SHIPPED,       // En tránsito/Enviada
    DELIVERED,     // Entregada
    CANCELLED,     // Cancelada
    REFUNDED       // Reembolsada
}

/**
 * Métodos de pago disponibles
 */
enum class PaymentMethod {
    CASH_ON_DELIVERY,  // Pago contra entrega
    CREDIT_CARD,       // Tarjeta de crédito
    DEBIT_CARD,        // Tarjeta de débito
    TRANSFER,          // Transferencia bancaria
    PSE,               // PSE Colombia
    PAYPAL,            // PayPal
    MERCADOPAGO        // MercadoPago
}

/**
 * Entidad Order - Representa una orden de compra
 */
@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // Relación con comprador
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    val buyer: User,

    // Relación con vendedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    // Número de orden único
    @Column(unique = true, nullable = false, length = 50)
    val orderNumber: String,

    // Estado de la orden
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: OrderStatus = OrderStatus.PENDING,

    // Montos
    @Column(nullable = false, precision = 15, scale = 2)
    val subtotal: BigDecimal, // Suma de items

    @Column(nullable = false, precision = 15, scale = 2)
    val discount: BigDecimal = BigDecimal.ZERO, // Descuentos aplicados

    @Column(nullable = false, precision = 15, scale = 2)
    val shipping: BigDecimal = BigDecimal.ZERO, // Costo de envío

    @Column(nullable = false, precision = 15, scale = 2)
    val tax: BigDecimal = BigDecimal.ZERO, // Impuestos

    @Column(nullable = false, precision = 15, scale = 2)
    val total: BigDecimal, // Total a pagar

    // Información de pago
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    var paymentMethod: PaymentMethod? = null,

    @Column(length = 50)
    var paymentStatus: String = "PENDING", // PENDING, PAID, FAILED, REFUNDED

    var paidAt: LocalDateTime? = null,

    // Dirección de envío
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

    // Información de envío
    @Column(length = 100)
    var trackingNumber: String? = null,

    @Column(length = 100)
    var carrier: String? = null, // Transportadora

    var shippedAt: LocalDateTime? = null,

    var deliveredAt: LocalDateTime? = null,

    // Notas y observaciones
    @Column(columnDefinition = "TEXT")
    var buyerNotes: String? = null,

    @Column(columnDefinition = "TEXT")
    var sellerNotes: String? = null,

    // Cancelación
    var cancelledAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var cancellationReason: String? = null,

    // Timestamps
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // Relación con items de la orden
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val items: MutableList<OrderItem> = mutableListOf()
) {
    /**
     * Hook que se ejecuta antes de actualizar la entidad
     */
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    /**
     * Cambia el estado de la orden
     */
    fun updateStatus(newStatus: OrderStatus) {
        status = newStatus

        when (newStatus) {
            OrderStatus.SHIPPED -> shippedAt = LocalDateTime.now()
            OrderStatus.DELIVERED -> deliveredAt = LocalDateTime.now()
            OrderStatus.CANCELLED -> cancelledAt = LocalDateTime.now()
            else -> {}
        }
    }

    /**
     * Marca la orden como pagada
     */
    fun markAsPaid(method: PaymentMethod) {
        paymentMethod = method
        paymentStatus = "PAID"
        paidAt = LocalDateTime.now()
    }

    /**
     * Verifica si la orden puede ser cancelada
     */
    fun canBeCancelled(): Boolean {
        return status in listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED)
    }

    /**
     * Verifica si la orden está en estado activo (no finalizada)
     */
    fun isActive(): Boolean {
        return status !in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)
    }
}
