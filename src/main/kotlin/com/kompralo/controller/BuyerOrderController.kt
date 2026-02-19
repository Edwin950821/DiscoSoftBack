package com.kompralo.controller

import com.kompralo.dto.OrderResponse
import com.kompralo.model.Order
import com.kompralo.model.OrderItem
import com.kompralo.dto.OrderItemResponse
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/buyer/orders")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class BuyerOrderController(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
) {

    @GetMapping
    fun getMyOrders(authentication: Authentication): ResponseEntity<*> {
        return try {
            val buyer = userRepository.findByEmail(authentication.name)
                .orElseThrow { RuntimeException("Usuario no encontrado") }

            val orders = orderRepository.findByBuyerOrderByCreatedAtDesc(buyer)
            ResponseEntity.ok(orders.map { it.toResponse() })
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener pedidos")))
        }
    }

    @GetMapping("/{id}")
    fun getMyOrder(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val buyer = userRepository.findByEmail(authentication.name)
                .orElseThrow { RuntimeException("Usuario no encontrado") }

            val order = orderRepository.findById(id)
                .orElseThrow { RuntimeException("Pedido no encontrado") }

            if (order.buyer.id != buyer.id) {
                throw RuntimeException("No autorizado para ver este pedido")
            }

            ResponseEntity.ok(order.toResponse())
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Pedido no encontrado")))
        }
    }

    private fun Order.toResponse(): OrderResponse {
        return OrderResponse(
            id = id!!,
            orderNumber = orderNumber,
            buyerId = buyer.id!!,
            buyerName = buyer.name,
            buyerEmail = buyer.email,
            status = status,
            items = items.map { it.toItemResponse() },
            subtotal = subtotal,
            discount = discount,
            shipping = shipping,
            tax = tax,
            total = total,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            paidAt = paidAt,
            shippingAddress = shippingAddress,
            shippingCity = shippingCity,
            shippingState = shippingState,
            shippingPostalCode = shippingPostalCode,
            shippingCountry = shippingCountry,
            shippingPhone = shippingPhone,
            trackingNumber = trackingNumber,
            carrier = carrier,
            shippedAt = shippedAt,
            deliveredAt = deliveredAt,
            buyerNotes = buyerNotes,
            sellerNotes = sellerNotes,
            cancelledAt = cancelledAt,
            cancellationReason = cancellationReason,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun OrderItem.toItemResponse(): OrderItemResponse {
        return OrderItemResponse(
            id = id!!,
            productId = productId,
            productName = productName,
            productSku = productSku,
            productImageUrl = productImageUrl,
            variantId = variantId,
            variantName = variantName,
            quantity = quantity,
            unitPrice = unitPrice,
            originalPrice = originalPrice,
            discount = discount,
            subtotal = subtotal,
        )
    }
}
