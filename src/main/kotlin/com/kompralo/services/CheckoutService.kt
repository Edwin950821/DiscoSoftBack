package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.StockRestockRepository
import com.kompralo.repository.InventoryMovementRepository
import com.kompralo.repository.ProductVariantRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Service
class CheckoutService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val stockRestockRepository: StockRestockRepository,
    private val inventoryMovementRepository: InventoryMovementRepository,
    private val pdfService: PdfService,
    private val emailService: EmailService,
    private val offerService: OfferService,
    private val wompiService: WompiService,
) {

    private val IVA_RATE = BigDecimal("0.19")
    private val COD_FEE = BigDecimal("5000")
    private val SHIPPING_BASE = BigDecimal("8000")

    @Transactional
    fun checkout(
        buyerEmail: String,
        request: CheckoutRequest,
        wompiTransactionId: String? = null,
    ): CheckoutResponse {
        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        if (request.items.isEmpty()) {
            throw RuntimeException("El carrito está vacío")
        }

        val paymentMethod: PaymentMethod? = request.paymentMethod?.let { pm ->
            try {
                PaymentMethod.valueOf(pm)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        val productIds = request.items.map { it.productId }
        val products = productRepository.findAllByIdWithSeller(productIds)
        val productMap = products.associateBy { it.id!! }

        // Pre-load variants for items that reference them
        val variantMap = mutableMapOf<Long, ProductVariant>()
        for (item in request.items) {
            if (item.quantity <= 0) {
                throw RuntimeException("La cantidad debe ser mayor a 0")
            }

            val product = productMap[item.productId]
                ?: throw RuntimeException("Producto con ID ${item.productId} no encontrado")

            if (product.status != ProductStatus.ACTIVE) {
                throw RuntimeException("El producto '${product.name}' no está disponible")
            }

            if (item.variantId != null) {
                val variant = productVariantRepository.findById(item.variantId)
                    .orElseThrow { RuntimeException("Variante no encontrada para '${product.name}'") }
                if (variant.product.id != product.id) {
                    throw RuntimeException("La variante no pertenece al producto '${product.name}'")
                }
                if (!variant.active) {
                    throw RuntimeException("La variante '${variant.name}' no está disponible")
                }
                if (variant.stock < item.quantity) {
                    throw RuntimeException("Stock insuficiente para '${product.name}' - ${variant.name}. Disponible: ${variant.stock}, Solicitado: ${item.quantity}")
                }
                variantMap[item.variantId] = variant
            } else {
                if (product.stock < item.quantity) {
                    throw RuntimeException("Stock insuficiente para '${product.name}'. Disponible: ${product.stock}, Solicitado: ${item.quantity}")
                }
            }
        }

        val itemsBySeller = request.items.groupBy { item ->
            productMap[item.productId]!!.seller.id!!
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
            val seller = productMap[sellerItems.first().productId]!!.seller
            val orderNumber = generateOrderNumber()

            var subtotal = BigDecimal.ZERO
            var orderDiscount = BigDecimal.ZERO
            var hasFreeShipping = false
            val orderItemsToCreate = mutableListOf<Pair<CheckoutItemRequest, Product>>()
            // Track offers applied per item for recording usage after order is saved
            val appliedOffers = mutableListOf<Triple<Offer, Product, BigDecimal>>()

            for (item in sellerItems) {
                val product = productMap[item.productId]!!
                val variant = item.variantId?.let { variantMap[it] }
                val effectivePrice = if (variant != null) product.price.add(variant.priceAdjustment) else product.price
                val itemTotal = effectivePrice.multiply(item.quantity.toBigDecimal())
                subtotal = subtotal.add(itemTotal)
                orderItemsToCreate.add(item to product)
            }

            // Check for applicable offers per item
            data class ItemOfferInfo(
                val offer: Offer?,
                val discount: BigDecimal,
                val discountPerUnit: BigDecimal
            )
            val itemOfferMap = mutableMapOf<Long, ItemOfferInfo>()

            for (item in sellerItems) {
                val product = productMap[item.productId]!!
                try {
                    val bestOffer = offerService.getBestOfferForProduct(product.id!!, item.quantity, buyer)
                    if (bestOffer != null && offerService.canUserUseOffer(bestOffer, buyer)) {
                        val discount = offerService.calculateDiscountForProduct(bestOffer, product, item.quantity)
                        if (discount > BigDecimal.ZERO) {
                            val discountPerUnit = discount.divide(item.quantity.toBigDecimal(), 2, RoundingMode.HALF_UP)
                            itemOfferMap[product.id!!] = ItemOfferInfo(bestOffer, discount, discountPerUnit)
                            orderDiscount = orderDiscount.add(discount)
                        }
                        if (bestOffer.type == OfferType.FREE_SHIPPING) {
                            hasFreeShipping = true
                        }
                    }
                } catch (e: Exception) {
                    println("[Checkout] Offer lookup error for product ${product.id}: ${e.message}")
                }
            }

            // Recalculate subtotal after discounts
            val discountedSubtotal = subtotal.subtract(orderDiscount)
            val tax = discountedSubtotal.multiply(IVA_RATE).setScale(0, RoundingMode.HALF_UP)
            val shipping = if (hasFreeShipping) BigDecimal.ZERO else SHIPPING_BASE
            val codFee = if (isCod) COD_FEE else BigDecimal.ZERO
            val total = discountedSubtotal.add(tax).add(shipping).add(codFee)

            val order = Order(
                buyer = buyer,
                seller = seller,
                orderNumber = orderNumber,
                status = OrderStatus.PENDING,
                subtotal = subtotal,
                discount = orderDiscount,
                shipping = shipping.add(codFee),
                tax = tax,
                total = total,
                paymentMethod = paymentMethod,
                shippingAddress = request.shippingAddress,
                shippingCity = request.shippingCity,
                shippingState = request.shippingState,
                shippingPostalCode = request.shippingPostalCode,
                shippingPhone = request.shippingPhone,
                buyerNotes = request.buyerNotes,
            )

            val savedOrder = orderRepository.save(order)

            for ((item, product) in orderItemsToCreate) {
                val variant = item.variantId?.let { variantMap[it] }
                val basePrice = if (variant != null) product.price.add(variant.priceAdjustment) else product.price
                val offerInfo = itemOfferMap[product.id!!]
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

                // Deduct stock from variant if present, otherwise from product
                if (variant != null) {
                    variant.stock -= item.quantity
                    productVariantRepository.save(variant)
                    // Also update product total stock
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

            if (paymentMethod != null && paymentMethod != PaymentMethod.CASH_ON_DELIVERY) {
                if (wompiTransactionId != null) {
                    savedOrder.markAsPaid(paymentMethod)
                    savedOrder.wompiTransactionId = wompiTransactionId
                }
            }

            val finalOrder = orderRepository.save(savedOrder)

            // Record offer usages
            for ((offer, _, discount) in appliedOffers) {
                try {
                    offerService.recordUsage(offer, buyer, finalOrder, discount)
                } catch (e: Exception) {
                    println("[Checkout] Offer usage recording error: ${e.message}")
                }
            }

            grandTotal = grandTotal.add(total)
            grandSubtotal = grandSubtotal.add(subtotal)
            grandDiscount = grandDiscount.add(orderDiscount)
            grandTax = grandTax.add(tax)
            grandShipping = grandShipping.add(shipping)
            grandCodFee = grandCodFee.add(codFee)

            orderSummaries.add(
                CheckoutOrderSummary(
                    orderId = finalOrder.id!!,
                    orderNumber = orderNumber,
                    sellerName = seller.name,
                    itemCount = sellerItems.size,
                    subtotal = subtotal,
                    discount = orderDiscount,
                    tax = tax,
                    shipping = shipping,
                    codFee = codFee,
                    total = total,
                )
            )

            try {
                notificationService.createAndSend(
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
            val itemCount = sellerItems.size

            CompletableFuture.runAsync {
                try {
                    val pdfBytes = pdfService.generateReceiptById(orderId)
                    if (pdfBytes != null) {
                        emailService.sendOrderConfirmationToBuyer(
                            buyerEmail = buyerEmailVal,
                            buyerName = buyerNameVal,
                            orderNumber = orderNumber,
                            total = total,
                            itemCount = itemCount,
                            sellerName = sellerNameVal,
                            pdfReceipt = pdfBytes
                        )
                    }

                    emailService.sendNewOrderNotificationToStore(
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
        // Prevent double-spend: check if this transaction was already used
        val existingOrder = orderRepository.findByWompiTransactionId(request.wompiTransactionId)
        if (existingOrder != null) {
            throw RuntimeException("Esta transaccion ya fue procesada (Pedido ${existingOrder.orderNumber})")
        }

        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val productIds = request.items.map { it.productId }
        val products = productRepository.findAllByIdWithSeller(productIds)
        val productMap = products.associateBy { it.id!! }

        val itemsBySeller = request.items.groupBy { item ->
            productMap[item.productId]!!.seller.id!!
        }

        var expectedTotal = BigDecimal.ZERO
        for ((_, sellerItems) in itemsBySeller) {
            var subtotal = BigDecimal.ZERO
            var orderDiscount = BigDecimal.ZERO
            var hasFreeShipping = false

            for (item in sellerItems) {
                val product = productMap[item.productId]!!
                val effectivePrice = if (item.variantId != null) {
                    val variant = productVariantRepository.findById(item.variantId).orElse(null)
                    if (variant != null) product.price.add(variant.priceAdjustment) else product.price
                } else product.price
                subtotal = subtotal.add(effectivePrice.multiply(item.quantity.toBigDecimal()))
            }

            for (item in sellerItems) {
                val product = productMap[item.productId]!!
                try {
                    val bestOffer = offerService.getBestOfferForProduct(product.id!!, item.quantity, buyer)
                    if (bestOffer != null && offerService.canUserUseOffer(bestOffer, buyer)) {
                        val discount = offerService.calculateDiscountForProduct(bestOffer, product, item.quantity)
                        if (discount > BigDecimal.ZERO) {
                            orderDiscount = orderDiscount.add(discount)
                        }
                        if (bestOffer.type == OfferType.FREE_SHIPPING) hasFreeShipping = true
                    }
                } catch (_: Exception) {}
            }

            val discountedSubtotal = subtotal.subtract(orderDiscount)
            val tax = discountedSubtotal.multiply(IVA_RATE).setScale(0, java.math.RoundingMode.HALF_UP)
            val shipping = if (hasFreeShipping) BigDecimal.ZERO else SHIPPING_BASE
            expectedTotal = expectedTotal.add(discountedSubtotal).add(tax).add(shipping)
        }

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

    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = UUID.randomUUID().toString().take(4).uppercase()
        return "ORD-$timestamp-$random"
    }
}
