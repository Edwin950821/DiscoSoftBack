package com.kompralo.dto

import java.math.BigDecimal

data class CheckoutRequest(
    val items: List<CheckoutItemRequest> = emptyList(),
    val shippingAddress: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPostalCode: String = "",
    val shippingPhone: String = "",
    val paymentMethod: String? = null,
    val buyerNotes: String? = null,
)

data class CheckoutItemRequest(
    val productId: Long,
    val quantity: Int,
)

data class CheckoutResponse(
    val orders: List<CheckoutOrderSummary>,
    val totalAmount: BigDecimal,
)

data class CheckoutOrderSummary(
    val orderId: Long,
    val orderNumber: String,
    val sellerName: String,
    val itemCount: Int,
    val total: BigDecimal,
)
