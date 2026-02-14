package com.kompralo.dto

import com.kompralo.model.OrderStatus
import com.kompralo.model.PaymentMethod
import java.math.BigDecimal
import java.time.LocalDateTime

// ==================== ORDER ITEM DTOs ====================

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val originalPrice: BigDecimal? = null,
    val discount: BigDecimal = BigDecimal.ZERO,
    val variantId: Long? = null,
    val variantName: String? = null
)

data class OrderItemResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productSku: String?,
    val productImageUrl: String?,
    val variantId: Long?,
    val variantName: String?,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val originalPrice: BigDecimal?,
    val discount: BigDecimal,
    val subtotal: BigDecimal
)

// ==================== ORDER DTOs ====================

data class CreateOrderRequest(
    val buyerEmail: String,
    val items: List<OrderItemRequest>,
    val paymentMethod: PaymentMethod? = null,
    val shippingAddress: String,
    val shippingCity: String,
    val shippingState: String,
    val shippingPostalCode: String,
    val shippingCountry: String = "Colombia",
    val shippingPhone: String,
    val discount: BigDecimal = BigDecimal.ZERO,
    val shipping: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val buyerNotes: String? = null,
    val sellerNotes: String? = null
)

data class UpdateOrderRequest(
    val paymentMethod: PaymentMethod? = null,
    val trackingNumber: String? = null,
    val carrier: String? = null,
    val sellerNotes: String? = null,
    val cancellationReason: String? = null
)

data class UpdateOrderStatusRequest(
    val status: OrderStatus
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val buyerId: Long,
    val buyerName: String,
    val buyerEmail: String,
    val status: OrderStatus,
    val items: List<OrderItemResponse>,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val shipping: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val paymentMethod: PaymentMethod?,
    val paymentStatus: String,
    val paidAt: LocalDateTime?,
    val shippingAddress: String,
    val shippingCity: String,
    val shippingState: String,
    val shippingPostalCode: String,
    val shippingCountry: String,
    val shippingPhone: String,
    val trackingNumber: String?,
    val carrier: String?,
    val shippedAt: LocalDateTime?,
    val deliveredAt: LocalDateTime?,
    val buyerNotes: String?,
    val sellerNotes: String?,
    val cancelledAt: LocalDateTime?,
    val cancellationReason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class OrderStatsResponse(
    val pending: Long,
    val confirmed: Long,
    val processing: Long,
    val shipped: Long,
    val delivered: Long,
    val cancelled: Long,
    val refunded: Long,
    val totalOrders: Long,
    val totalRevenue: BigDecimal
)
