package com.kompralo.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class WompiInitRequest(
    val items: List<CheckoutItemRequest> = emptyList(),
    val paymentMethod: String = "",
    val shippingAddress: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPostalCode: String = "",
    val shippingPhone: String = "",
    val buyerNotes: String? = null,
)

data class WompiInitResponse(
    val reference: String,
    val amountInCents: Long,
    val currency: String = "COP",
    val integritySignature: String,
    val publicKey: String,
    val taxInCents: Long,
    val redirectUrl: String,
    val customerData: WompiCustomerData,
    val shippingAddress: WompiShippingAddress,
)

data class WompiCustomerData(
    val email: String,
    val fullName: String,
    val phoneNumber: String,
    val phoneNumberPrefix: String = "+57",
)

data class WompiShippingAddress(
    val addressLine1: String,
    val city: String,
    val country: String = "CO",
    val region: String,
    val name: String,
    val phoneNumber: String,
    val postalCode: String,
)

data class WompiConfirmRequest(
    val wompiTransactionId: String,
    val reference: String,
    val items: List<CheckoutItemRequest> = emptyList(),
    val shippingAddress: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPostalCode: String = "",
    val shippingPhone: String = "",
    val paymentMethod: String = "",
    val buyerNotes: String? = null,
)

data class WompiTransactionResponse(
    val data: WompiTransactionData,
)

data class WompiTransactionData(
    val id: String,
    val status: String,
    @JsonProperty("amount_in_cents") val amountInCents: Long,
    val currency: String,
    val reference: String,
)
