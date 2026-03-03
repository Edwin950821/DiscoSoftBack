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
    val variantId: Long? = null,
)

data class CheckoutResponse(
    val orders: List<CheckoutOrderSummary>,
    val totalAmount: BigDecimal,
    val subtotalAmount: BigDecimal,
    val totalTax: BigDecimal,
    val totalShipping: BigDecimal,
    val totalCodFee: BigDecimal,
    val totalDiscount: BigDecimal = BigDecimal.ZERO,
)

data class CheckoutOrderSummary(
    val orderId: Long,
    val orderNumber: String,
    val sellerName: String,
    val itemCount: Int,
    val subtotal: BigDecimal,
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal,
    val shipping: BigDecimal,
    val codFee: BigDecimal,
    val total: BigDecimal,
)
