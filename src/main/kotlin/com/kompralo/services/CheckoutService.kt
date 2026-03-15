package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.StockRestockRepository
import com.kompralo.repository.InventoryMovementRepository
import com.kompralo.repository.ProductVariantRepository
import com.kompralo.port.EmailPort
import com.kompralo.port.NotificationPort
import com.kompralo.port.OfferCalculationPort
import com.kompralo.port.PdfPort
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Service
class CheckoutService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val userRepository: UserRepository,
    private val notificationPort: NotificationPort,
    private val stockRestockRepository: StockRestockRepository,
    private val inventoryMovementRepository: InventoryMovementRepository,
    private val pdfPort: PdfPort,
    private val emailPort: EmailPort,
    private val offerCalculation: OfferCalculationPort,
    private val wompiService: WompiService,
    private val stockValidation: StockValidationService,
    private val priceCalculation: PriceCalculationService,
) {

    @Transactional
    fun checkout(
        buyerEmail: String,
        request: CheckoutRequest,
        wompiTransactionId: String? = null,
    ): CheckoutResponse {
        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { EntityNotFoundException("Usuario", buyerEmail) }

        val paymentMethod: PaymentMethod? = request.paymentMethod?.let { pm ->
            try { PaymentMethod.valueOf(pm) } catch (_: IllegalArgumentException) { null }
        }

        val cart = stockValidation.validateAndLoad(request.items)
        val itemsBySeller = request.items.groupBy { item ->
            cart.productMap[item.productId]!!.seller.id!!
        }

        val isCod = paymentMethod == PaymentMethod.CASH_ON_DELIVERY
        val orderSummaries = mutableListOf<CheckoutOrderSummary>()
        var grandTotal = BigDecimal.ZERO
        var grandSubtotal = BigDecimal.ZERO
        var grandTax = BigDecimal.ZERO
        var grandShipping = BigDecimal.ZERO
        var grandCodFee = BigDecimal.ZERO
        var grandDiscount = BigDecimal.ZERO

        for ((sellerId, sellerItems) in itemsBySeller) {
            val seller = cart.productMap[sellerItems.first().productId]!!.seller
            val orderNumber = generateOrderNumber()

            val pricing = priceCalculation.calculateForSeller(
                sellerItems, cart.productMap, cart.variantMap, buyer, isCod
            )

            val order = Order(
                buyer = buyer,
                seller = seller,
                orderNumber = orderNumber,
                status = OrderStatus.PENDING,
                subtotal = pricing.subtotal,
                discount = pricing.discount,
                shipping = pricing.shipping.add(pricing.codFee),
                tax = pricing.tax,
                total = pricing.total,
                paymentMethod = paymentMethod,
                shippingAddress = request.shippingAddress,
                shippingCity = request.shippingCity,
                shippingState = request.shippingState,
                shippingPostalCode = request.shippingPostalCode,
                shippingPhone = request.shippingPhone,
                buyerNotes = request.buyerNotes,
            )

            val savedOrder = orderRepository.save(order)
            val appliedOffers = mutableListOf<Triple<Offer, Product, BigDecimal>>()

            for (item in sellerItems) {
                val product = cart.productMap[item.productId]!!
                val variant = item.variantId?.let { cart.variantMap[it] }
                val basePrice = if (variant != null) product.price.add(variant.priceAdjustment) else product.price
                val offerInfo = pricing.itemOfferMap[product.id!!]
                val unitPrice = if (offerInfo != null) basePrice.subtract(offerInfo.discountPerUnit) else basePrice
                val itemDiscount = offerInfo?.discount ?: BigDecimal.ZERO
                val itemSubtotal = unitPrice.multiply(item.quantity.toBigDecimal())

                val orderItem = OrderItem(
                    order = savedOrder,
                    productId = product.id!!,
                    productName = product.name,
                    productSku = product.sku,
                    productImageUrl = variant?.imageUrl ?: product.imageUrl,
                    variantId = variant?.id,
                    variantName = variant?.name,
                    quantity = item.quantity,
                    unitPrice = unitPrice,
                    originalPrice = if (offerInfo != null) basePrice else null,
                    discount = itemDiscount,
                    subtotal = itemSubtotal,
                )
                savedOrder.items.add(orderItem)

                if (offerInfo?.offer != null) {
                    appliedOffers.add(Triple(offerInfo.offer, product, offerInfo.discount))
                }

                deductStock(item, product, variant, buyer, savedOrder, orderNumber)
            }

            if (paymentMethod != null && paymentMethod != PaymentMethod.CASH_ON_DELIVERY) {
                if (wompiTransactionId != null) {
                    savedOrder.markAsPaid(paymentMethod)
                    savedOrder.wompiTransactionId = wompiTransactionId
                }
            }

            val finalOrder = orderRepository.save(savedOrder)
            recordOfferUsages(appliedOffers, buyer, finalOrder)

            grandTotal = grandTotal.add(pricing.total)
            grandSubtotal = grandSubtotal.add(pricing.subtotal)
            grandDiscount = grandDiscount.add(pricing.discount)
            grandTax = grandTax.add(pricing.tax)
            grandShipping = grandShipping.add(pricing.shipping)
            grandCodFee = grandCodFee.add(pricing.codFee)

            orderSummaries.add(
                CheckoutOrderSummary(
                    orderId = finalOrder.id!!,
                    orderNumber = orderNumber,
                    sellerName = seller.name,
                    itemCount = sellerItems.size,
                    subtotal = pricing.subtotal,
                    discount = pricing.discount,
                    tax = pricing.tax,
                    shipping = pricing.shipping,
                    codFee = pricing.codFee,
                    total = pricing.total,
                )
            )

            sendPostOrderNotifications(sellerId, buyer, seller, finalOrder, orderNumber, sellerItems.size, pricing.total)
        }

        return CheckoutResponse(
            orders = orderSummaries,
            totalAmount = grandTotal,
            subtotalAmount = grandSubtotal,
            totalTax = grandTax,
            totalShipping = grandShipping,
            totalCodFee = grandCodFee,
            totalDiscount = grandDiscount,
        )
    }

    @Transactional
    fun checkoutWithWompi(buyerEmail: String, request: WompiConfirmRequest): CheckoutResponse {
        val existingOrder = orderRepository.findByWompiTransactionId(request.wompiTransactionId)
        if (existingOrder != null) {
            throw BusinessRuleViolationException("Esta transaccion ya fue procesada (Pedido ${existingOrder.orderNumber})")
        }

        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { EntityNotFoundException("Usuario", buyerEmail) }

        val cart = stockValidation.validateAndLoad(request.items)
        val itemsBySeller = request.items.groupBy { item ->
            cart.productMap[item.productId]!!.seller.id!!
        }

        val expectedTotal = priceCalculation.calculateExpectedTotal(
            itemsBySeller, cart.productMap, cart.variantMap, buyer
        )

        val expectedAmountInCents = wompiService.toCents(expectedTotal)
        wompiService.verifyTransaction(request.wompiTransactionId, expectedAmountInCents, request.reference)

        val checkoutRequest = CheckoutRequest(
            items = request.items,
            shippingAddress = request.shippingAddress,
            shippingCity = request.shippingCity,
            shippingState = request.shippingState,
            shippingPostalCode = request.shippingPostalCode,
            shippingPhone = request.shippingPhone,
            paymentMethod = request.paymentMethod,
            buyerNotes = request.buyerNotes,
        )

        return checkout(buyerEmail, checkoutRequest, request.wompiTransactionId)
    }

    private fun deductStock(
        item: CheckoutItemRequest,
        product: Product,
        variant: ProductVariant?,
        buyer: User,
        savedOrder: Order,
        orderNumber: String
    ) {
        if (variant != null) {
            variant.stock -= item.quantity
            productVariantRepository.save(variant)
            product.stock = product.variants.sumOf { it.stock }
        } else {
            product.stock -= item.quantity
        }
        product.sales += item.quantity
        product.updateStockStatus()
        productRepository.save(product)

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

    private fun recordOfferUsages(
        appliedOffers: List<Triple<Offer, Product, BigDecimal>>,
        buyer: User,
        finalOrder: Order
    ) {
        for ((offer, _, discount) in appliedOffers) {
            try {
                offerCalculation.recordUsage(offer, buyer, finalOrder, discount)
            } catch (e: Exception) {
                println("[Checkout] Offer usage recording error: ${e.message}")
            }
        }
    }

    private fun sendPostOrderNotifications(
        sellerId: Long,
        buyer: User,
        seller: User,
        finalOrder: Order,
        orderNumber: String,
        itemCount: Int,
        total: BigDecimal
    ) {
        try {
            notificationPort.createAndSend(
                userId = sellerId,
                type = NotificationType.NEW_ORDER,
                title = "Nuevo pedido recibido",
                message = "Pedido $orderNumber de ${buyer.name} por \$${total}.",
                priority = "high",
                actionUrl = "/admin/orders",
                relatedEntityId = finalOrder.id,
                relatedEntityType = RelatedEntityType.ORDER,
            )
        } catch (e: Exception) {
            println("[Checkout] Error enviando notificación: ${e.message}")
        }

        val buyerEmailVal = buyer.email
        val buyerNameVal = buyer.name
        val sellerEmailVal = seller.email
        val sellerNameVal = seller.name
        val orderId = finalOrder.id!!

        CompletableFuture.runAsync {
            try {
                val pdfBytes = pdfPort.generateReceiptById(orderId)
                if (pdfBytes != null) {
                    emailPort.sendOrderConfirmationToBuyer(
                        buyerEmail = buyerEmailVal,
                        buyerName = buyerNameVal,
                        orderNumber = orderNumber,
                        total = total,
                        itemCount = itemCount,
                        sellerName = sellerNameVal,
                        pdfReceipt = pdfBytes
                    )
                }

                emailPort.sendNewOrderNotificationToStore(
                    sellerEmail = sellerEmailVal,
                    sellerName = sellerNameVal,
                    orderNumber = orderNumber,
                    buyerName = buyerNameVal,
                    total = total,
                    itemCount = itemCount
                )
            } catch (e: Exception) {
                println("[Checkout] Error enviando emails: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().take(4).uppercase()
        return "ORD-$timestamp-$random"
    }
}
