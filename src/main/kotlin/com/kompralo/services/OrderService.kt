package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.OrderItemRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {

    companion object {
        private val VALID_TRANSITIONS = mapOf(
            OrderStatus.PENDING to listOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED to listOf(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING to listOf(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED to listOf(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED to listOf(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED to emptyList(),
            OrderStatus.REFUNDED to emptyList()
        )
    }

    fun getOrdersBySeller(email: String, status: OrderStatus?, search: String?): List<OrderResponse> {
        val seller = findSellerByEmail(email)

        val orders = when {
            !search.isNullOrBlank() -> orderRepository.searchBySellerAndText(seller, search)
            status != null -> orderRepository.findBySellerAndStatus(seller, status)
            else -> orderRepository.findBySellerOrderByCreatedAtDesc(seller)
        }

        return orders.map { it.toResponse() }
    }

    fun getOrderById(id: Long, sellerId: Long): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (order.seller.id != sellerId) {
            throw RuntimeException("No autorizado para ver este pedido")
        }

        return order.toResponse()
    }

    @Transactional
    fun createOrder(sellerId: Long, request: CreateOrderRequest): OrderResponse {
        val seller = userRepository.findById(sellerId)
            .orElseThrow { RuntimeException("Vendedor no encontrado") }

        val buyer = userRepository.findByEmail(request.buyerEmail)
            .orElseThrow { RuntimeException("Comprador con email '${request.buyerEmail}' no encontrado. El comprador debe estar registrado.") }

        if (request.items.isEmpty()) {
            throw RuntimeException("El pedido debe tener al menos un producto")
        }

        val orderNumber = generateOrderNumber()

        // Calcular subtotal desde los items
        var subtotal = BigDecimal.ZERO
        val orderItems = mutableListOf<OrderItem>()

        for (itemRequest in request.items) {
            if (itemRequest.quantity <= 0) {
                throw RuntimeException("La cantidad debe ser mayor a 0")
            }

            val product = productRepository.findById(itemRequest.productId)
                .orElseThrow { RuntimeException("Producto con ID ${itemRequest.productId} no encontrado") }

            if (product.seller.id != sellerId) {
                throw RuntimeException("El producto '${product.name}' no pertenece a tu tienda")
            }

            if (product.stock < itemRequest.quantity) {
                throw RuntimeException("Stock insuficiente para '${product.name}'. Disponible: ${product.stock}, Solicitado: ${itemRequest.quantity}")
            }

            val itemSubtotal = itemRequest.unitPrice.multiply(itemRequest.quantity.toBigDecimal()).subtract(itemRequest.discount)
            subtotal = subtotal.add(itemSubtotal)

            // Descontar stock
            product.stock -= itemRequest.quantity
            product.updateStockStatus()
            productRepository.save(product)
        }

        val total = subtotal.subtract(request.discount).add(request.shipping).add(request.tax)

        val order = Order(
            buyer = buyer,
            seller = seller,
            orderNumber = orderNumber,
            status = OrderStatus.PENDING,
            subtotal = subtotal,
            discount = request.discount,
            shipping = request.shipping,
            tax = request.tax,
            total = total,
            paymentMethod = request.paymentMethod,
            shippingAddress = request.shippingAddress,
            shippingCity = request.shippingCity,
            shippingState = request.shippingState,
            shippingPostalCode = request.shippingPostalCode,
            shippingCountry = request.shippingCountry,
            shippingPhone = request.shippingPhone,
            buyerNotes = request.buyerNotes,
            sellerNotes = request.sellerNotes
        )

        val savedOrder = orderRepository.save(order)

        // Crear items con snapshot de producto
        for (itemRequest in request.items) {
            val product = productRepository.findById(itemRequest.productId).get()
            val itemSubtotal = itemRequest.unitPrice.multiply(itemRequest.quantity.toBigDecimal()).subtract(itemRequest.discount)

            val orderItem = OrderItem(
                order = savedOrder,
                productId = product.id!!,
                productName = product.name,
                productSku = product.sku,
                productImageUrl = product.imageUrl,
                variantId = itemRequest.variantId,
                variantName = itemRequest.variantName,
                quantity = itemRequest.quantity,
                unitPrice = itemRequest.unitPrice,
                originalPrice = itemRequest.originalPrice,
                discount = itemRequest.discount,
                subtotal = itemSubtotal
            )
            savedOrder.items.add(orderItem)
        }

        val finalOrder = orderRepository.save(savedOrder)

        notificationService.createAndSend(
            userId = sellerId,
            type = NotificationType.NEW_ORDER,
            title = "Nuevo pedido creado",
            message = "Pedido $orderNumber creado para ${buyer.name} por \$${total}.",
            priority = "medium",
            actionUrl = "/admin/orders",
            relatedEntityId = finalOrder.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return finalOrder.toResponse()
    }

    @Transactional
    fun updateOrderStatus(id: Long, sellerId: Long, newStatus: OrderStatus): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (order.seller.id != sellerId) {
            throw RuntimeException("No autorizado para actualizar este pedido")
        }

        val validNextStatuses = VALID_TRANSITIONS[order.status] ?: emptyList()
        if (newStatus !in validNextStatuses) {
            throw RuntimeException("No se puede cambiar de ${order.status} a $newStatus. Transiciones válidas: $validNextStatuses")
        }

        order.updateStatus(newStatus)
        val saved = orderRepository.save(order)

        // Enviar notificación según el nuevo estado
        val (notifType, notifTitle, notifMessage) = when (newStatus) {
            OrderStatus.CONFIRMED -> Triple(
                NotificationType.ORDER_CONFIRMED,
                "Pedido confirmado",
                "El pedido ${order.orderNumber} ha sido confirmado."
            )
            OrderStatus.SHIPPED -> Triple(
                NotificationType.ORDER_SHIPPED,
                "Pedido enviado",
                "El pedido ${order.orderNumber} ha sido enviado."
            )
            OrderStatus.DELIVERED -> Triple(
                NotificationType.ORDER_DELIVERED,
                "Pedido entregado",
                "El pedido ${order.orderNumber} ha sido entregado."
            )
            OrderStatus.CANCELLED -> Triple(
                NotificationType.ORDER_CANCELLED,
                "Pedido cancelado",
                "El pedido ${order.orderNumber} ha sido cancelado."
            )
            else -> Triple(
                NotificationType.NEW_ORDER,
                "Pedido actualizado",
                "El pedido ${order.orderNumber} cambió a estado $newStatus."
            )
        }

        notificationService.createAndSend(
            userId = sellerId,
            type = notifType,
            title = notifTitle,
            message = notifMessage,
            priority = if (newStatus == OrderStatus.CANCELLED) "high" else "medium",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun updateOrder(id: Long, sellerId: Long, request: UpdateOrderRequest): OrderResponse {
        val order = orderRepository.findById(id)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (order.seller.id != sellerId) {
            throw RuntimeException("No autorizado para actualizar este pedido")
        }

        request.paymentMethod?.let { order.paymentMethod = it }
        request.trackingNumber?.let { order.trackingNumber = it }
        request.carrier?.let { order.carrier = it }
        request.sellerNotes?.let { order.sellerNotes = it }
        request.cancellationReason?.let { order.cancellationReason = it }

        val saved = orderRepository.save(order)
        return saved.toResponse()
    }

    @Transactional
    fun deleteOrder(id: Long, sellerId: Long) {
        val order = orderRepository.findById(id)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (order.seller.id != sellerId) {
            throw RuntimeException("No autorizado para eliminar este pedido")
        }

        if (order.status !in listOf(OrderStatus.PENDING, OrderStatus.CANCELLED)) {
            throw RuntimeException("Solo se pueden eliminar pedidos en estado PENDING o CANCELLED. Estado actual: ${order.status}")
        }

        val orderNumber = order.orderNumber
        orderRepository.delete(order)

        notificationService.createAndSend(
            userId = sellerId,
            type = NotificationType.ORDER_CANCELLED,
            title = "Pedido eliminado",
            message = "El pedido $orderNumber ha sido eliminado.",
            priority = "low",
            relatedEntityType = RelatedEntityType.ORDER
        )
    }

    fun getOrderStats(email: String): OrderStatsResponse {
        val seller = findSellerByEmail(email)

        val pending = orderRepository.countBySellerAndStatus(seller, OrderStatus.PENDING)
        val confirmed = orderRepository.countBySellerAndStatus(seller, OrderStatus.CONFIRMED)
        val processing = orderRepository.countBySellerAndStatus(seller, OrderStatus.PROCESSING)
        val shipped = orderRepository.countBySellerAndStatus(seller, OrderStatus.SHIPPED)
        val delivered = orderRepository.countBySellerAndStatus(seller, OrderStatus.DELIVERED)
        val cancelled = orderRepository.countBySellerAndStatus(seller, OrderStatus.CANCELLED)
        val refunded = orderRepository.countBySellerAndStatus(seller, OrderStatus.REFUNDED)
        val totalOrders = orderRepository.countBySeller(seller)

        // Calcular revenue total (órdenes pagadas, no canceladas ni reembolsadas)
        val now = LocalDateTime.now()
        val yearStart = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0)
        val totalRevenue = orderRepository.sumTotalBySellerAndDateRange(seller, yearStart, now)

        return OrderStatsResponse(
            pending = pending,
            confirmed = confirmed,
            processing = processing,
            shipped = shipped,
            delivered = delivered,
            cancelled = cancelled,
            refunded = refunded,
            totalOrders = totalOrders,
            totalRevenue = totalRevenue
        )
    }

    fun getOrdersForExport(
        email: String,
        status: OrderStatus?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<OrderResponse> {
        val seller = findSellerByEmail(email)
        val orders = orderRepository.findBySellerFiltered(seller, status, startDate, endDate)
        return orders.map { it.toResponse() }
    }

    // ==================== HELPERS ====================

    private fun findSellerByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
    }

    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().take(4).uppercase()
        return "ORD-$timestamp-$random"
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
            updatedAt = updatedAt
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
            subtotal = subtotal
        )
    }
}
