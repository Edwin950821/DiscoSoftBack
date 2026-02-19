package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.StockRestockRepository
import com.kompralo.repository.InventoryMovementRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class CheckoutService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val stockRestockRepository: StockRestockRepository,
    private val inventoryMovementRepository: InventoryMovementRepository,
) {

    @Transactional
    fun checkout(buyerEmail: String, request: CheckoutRequest): CheckoutResponse {
        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        if (request.items.isEmpty()) {
            throw RuntimeException("El carrito está vacío")
        }

        // Parse payment method from string
        val paymentMethod: PaymentMethod? = request.paymentMethod?.let { pm ->
            try {
                PaymentMethod.valueOf(pm)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        // Fetch all products with seller eagerly loaded to avoid lazy loading issues
        val productIds = request.items.map { it.productId }
        val products = productRepository.findAllByIdWithSeller(productIds)
        val productMap = products.associateBy { it.id!! }

        // Validate each item
        for (item in request.items) {
            if (item.quantity <= 0) {
                throw RuntimeException("La cantidad debe ser mayor a 0")
            }

            val product = productMap[item.productId]
                ?: throw RuntimeException("Producto con ID ${item.productId} no encontrado")

            if (product.status != ProductStatus.ACTIVE) {
                throw RuntimeException("El producto '${product.name}' no está disponible")
            }

            if (product.stock < item.quantity) {
                throw RuntimeException("Stock insuficiente para '${product.name}'. Disponible: ${product.stock}, Solicitado: ${item.quantity}")
            }
        }

        // Group items by seller
        val itemsBySeller = request.items.groupBy { item ->
            productMap[item.productId]!!.seller.id!!
        }

        val orderSummaries = mutableListOf<CheckoutOrderSummary>()
        var grandTotal = BigDecimal.ZERO

        for ((sellerId, sellerItems) in itemsBySeller) {
            val seller = productMap[sellerItems.first().productId]!!.seller
            val orderNumber = generateOrderNumber()

            // Calculate subtotal for this seller group
            var subtotal = BigDecimal.ZERO
            val orderItemsToCreate = mutableListOf<Pair<CheckoutItemRequest, Product>>()

            for (item in sellerItems) {
                val product = productMap[item.productId]!!
                val itemTotal = product.price.multiply(item.quantity.toBigDecimal())
                subtotal = subtotal.add(itemTotal)
                orderItemsToCreate.add(item to product)
            }

            // Create order
            val order = Order(
                buyer = buyer,
                seller = seller,
                orderNumber = orderNumber,
                status = OrderStatus.PENDING,
                subtotal = subtotal,
                discount = BigDecimal.ZERO,
                shipping = BigDecimal.ZERO,
                tax = BigDecimal.ZERO,
                total = subtotal,
                paymentMethod = paymentMethod,
                shippingAddress = request.shippingAddress,
                shippingCity = request.shippingCity,
                shippingState = request.shippingState,
                shippingPostalCode = request.shippingPostalCode,
                shippingPhone = request.shippingPhone,
                buyerNotes = request.buyerNotes,
            )

            val savedOrder = orderRepository.save(order)

            // Create order items + deduct stock
            for ((item, product) in orderItemsToCreate) {
                val itemSubtotal = product.price.multiply(item.quantity.toBigDecimal())

                val orderItem = OrderItem(
                    order = savedOrder,
                    productId = product.id!!,
                    productName = product.name,
                    productSku = product.sku,
                    productImageUrl = product.imageUrl,
                    quantity = item.quantity,
                    unitPrice = product.price,
                    subtotal = itemSubtotal,
                )
                savedOrder.items.add(orderItem)

                // Deduct stock
                product.stock -= item.quantity
                product.sales += item.quantity
                product.updateStockStatus()
                productRepository.save(product)

                // FIFO: deduct from oldest restock batches
                try {
                    val restocks = stockRestockRepository.findByProductIdWithRemainingFIFO(product.id!!)
                    var remaining = item.quantity
                    for (sr in restocks) {
                        if (remaining <= 0) break
                        val deduct = minOf(remaining, sr.quantityRemaining)
                        sr.quantityRemaining -= deduct
                        sr.quantitySold += deduct
                        remaining -= deduct
                        stockRestockRepository.save(sr)
                    }
                } catch (e: Exception) {
                    println("[Checkout] FIFO deduction error: ${e.message}")
                }

                // Record SALE movement
                try {
                    inventoryMovementRepository.save(
                        InventoryMovement(
                            product = product,
                            movementType = "SALE",
                            quantity = -item.quantity,
                            resultingStock = product.stock,
                            user = buyer,
                            referenceType = "ORDER",
                            referenceId = savedOrder.id,
                            notes = "Venta - Pedido $orderNumber",
                        )
                    )
                } catch (e: Exception) {
                    println("[Checkout] Movement recording error: ${e.message}")
                }
            }

            // Auto-mark as PAID for online payment methods (not cash on delivery)
            if (paymentMethod != null && paymentMethod != PaymentMethod.CASH_ON_DELIVERY) {
                savedOrder.markAsPaid(paymentMethod)
            }

            val finalOrder = orderRepository.save(savedOrder)
            grandTotal = grandTotal.add(subtotal)

            orderSummaries.add(
                CheckoutOrderSummary(
                    orderId = finalOrder.id!!,
                    orderNumber = orderNumber,
                    sellerName = seller.name,
                    itemCount = sellerItems.size,
                    total = subtotal,
                )
            )

            // Notify seller (non-blocking)
            try {
                notificationService.createAndSend(
                    userId = sellerId,
                    type = NotificationType.NEW_ORDER,
                    title = "Nuevo pedido recibido",
                    message = "Pedido $orderNumber de ${buyer.name} por \$${subtotal}.",
                    priority = "high",
                    actionUrl = "/admin/orders",
                    relatedEntityId = finalOrder.id,
                    relatedEntityType = RelatedEntityType.ORDER,
                )
            } catch (e: Exception) {
                println("[Checkout] Error enviando notificación: ${e.message}")
            }
        }

        return CheckoutResponse(
            orders = orderSummaries,
            totalAmount = grandTotal,
        )
    }

    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().take(4).uppercase()
        return "ORD-$timestamp-$random"
    }
}
