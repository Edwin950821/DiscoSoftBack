package com.kompralo.controller

import com.kompralo.dto.OrderResponse
import com.kompralo.model.Order
import com.kompralo.model.OrderItem
import com.kompralo.model.OrderStatus
import com.kompralo.dto.OrderItemResponse
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.UserRepository
import com.kompralo.services.NotificationService
import com.kompralo.model.NotificationType
import com.kompralo.model.PaymentMethod
import com.kompralo.model.RelatedEntityType
import com.kompralo.services.PdfService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/buyer/orders")
class BuyerOrderController(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val pdfService: PdfService,
    private val notificationService: NotificationService,
) {

    @PatchMapping("/{id}/confirm-delivery")
    @Transactional
    fun confirmDelivery(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val buyer = userRepository.findByEmail(authentication.name)
                .orElseThrow { RuntimeException("Usuario no encontrado") }

            val order = orderRepository.findById(id)
                .orElseThrow { RuntimeException("Pedido no encontrado") }

            if (order.buyer.id != buyer.id) {
                throw RuntimeException("No autorizado")
            }

            if (order.status != OrderStatus.SHIPPED) {
                throw RuntimeException("Solo se puede confirmar la recepcion de pedidos enviados (estado actual: ${order.status})")
            }

            order.updateStatus(OrderStatus.DELIVERED)

            if (order.paymentMethod == PaymentMethod.CASH_ON_DELIVERY && order.paymentStatus != "PAID") {
                order.markAsPaid(PaymentMethod.CASH_ON_DELIVERY)
            }

            val saved = orderRepository.save(order)

            val paymentNote = if (order.paymentMethod == PaymentMethod.CASH_ON_DELIVERY)
                " Pago contra entrega confirmado." else ""

            notificationService.createAndSend(
                userId = order.seller.id!!,
                type = NotificationType.ORDER_DELIVERED,
                title = "Pedido entregado",
                message = "El comprador confirmo la recepcion del pedido ${order.orderNumber}.$paymentNote",
                priority = "medium",
                actionUrl = "/admin/orders",
                relatedEntityId = saved.id,
                relatedEntityType = RelatedEntityType.ORDER
            )

            ResponseEntity.ok(saved.toResponse())
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al confirmar recepcion")))
        }
    }

    @PatchMapping("/{id}/cancel")
    @Transactional
    fun cancelOrder(
        @PathVariable id: Long,
        @RequestBody body: Map<String, String>,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val buyer = userRepository.findByEmail(authentication.name)
                .orElseThrow { RuntimeException("Usuario no encontrado") }

            val order = orderRepository.findById(id)
                .orElseThrow { RuntimeException("Pedido no encontrado") }

            if (order.buyer.id != buyer.id) {
                throw RuntimeException("No autorizado para cancelar este pedido")
            }

            if (!order.canBeCancelled()) {
                throw RuntimeException("Este pedido no puede ser cancelado (estado: ${order.status})")
            }

            // Restore stock for each item
            for (item in order.items) {
                val product = productRepository.findById(item.productId).orElse(null)
                if (product != null) {
                    product.stock += item.quantity
                    product.sales = (product.sales - item.quantity).coerceAtLeast(0)
                    product.updateStockStatus()
                    productRepository.save(product)
                }
            }

            order.updateStatus(OrderStatus.CANCELLED)
            order.cancellationReason = body["reason"]
            val saved = orderRepository.save(order)

            ResponseEntity.ok(saved.toResponse())
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al cancelar pedido")))
        }
    }

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

    @GetMapping("/{orderNumber}/receipt")
    fun downloadReceipt(
        @PathVariable orderNumber: String,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val buyer = userRepository.findByEmail(authentication.name)
                .orElseThrow { RuntimeException("Usuario no encontrado") }

            val order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                ?: throw RuntimeException("Pedido no encontrado")

            if (order.buyer.id != buyer.id) {
                throw RuntimeException("No autorizado para descargar este comprobante")
            }

            val pdfBytes = pdfService.generateReceipt(order)

            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Comprobante_${orderNumber}.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.size.toLong())
                .body(pdfBytes)
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al generar comprobante")))
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
            estimatedDeliveryDate = estimatedDeliveryDate,
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
