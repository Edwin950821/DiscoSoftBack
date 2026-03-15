package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.config.ShippingConfiguration
import com.kompralo.config.TaxConfiguration
import com.kompralo.dto.WompiCustomerData
import com.kompralo.dto.WompiInitRequest
import com.kompralo.dto.WompiInitResponse
import com.kompralo.dto.WompiShippingAddress
import com.kompralo.model.OfferType
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class WompiInitService(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val offerService: OfferService,
    private val wompiService: WompiService,
    private val taxConfig: TaxConfiguration,
    private val shippingConfig: ShippingConfiguration,
) {
    @Value("\${app.frontend-url}")
    private lateinit var frontendUrl: String

    fun initializePayment(buyerEmail: String, request: WompiInitRequest): WompiInitResponse {
        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { EntityNotFoundException("Usuario", buyerEmail) }

        if (request.items.isEmpty()) {
            throw ValidationException("El carrito esta vacio")
        }

        val productIds = request.items.map { it.productId }
        val products = productRepository.findAllByIdWithSeller(productIds)
        val productMap = products.associateBy { it.id!! }

        for (item in request.items) {
            if (item.quantity <= 0) {
                throw ValidationException("La cantidad debe ser mayor a 0")
            }
            val product = productMap[item.productId]
                ?: throw EntityNotFoundException("Producto", item.productId)
            if (product.status != com.kompralo.model.ProductStatus.ACTIVE) {
                throw BusinessRuleViolationException("El producto '${product.name}' no esta disponible")
            }
            if (product.stock < item.quantity) {
                throw BusinessRuleViolationException("Stock insuficiente para '${product.name}'")
            }
        }

        val itemsBySeller = request.items.groupBy { item ->
            productMap[item.productId]!!.seller.id!!
        }

        var grandTotal = BigDecimal.ZERO
        var grandTax = BigDecimal.ZERO

        for ((_, sellerItems) in itemsBySeller) {
            var subtotal = BigDecimal.ZERO
            var orderDiscount = BigDecimal.ZERO
            var hasFreeShipping = false

            for (item in sellerItems) {
                val product = productMap[item.productId]!!
                subtotal = subtotal.add(product.price.multiply(item.quantity.toBigDecimal()))
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
                        if (bestOffer.type == OfferType.FREE_SHIPPING) {
                            hasFreeShipping = true
                        }
                    }
                } catch (_: Exception) {}
            }

            val discountedSubtotal = subtotal.subtract(orderDiscount)
            val tax = discountedSubtotal.multiply(taxConfig.ivaRate).setScale(0, RoundingMode.HALF_UP)
            val shipping = if (hasFreeShipping) BigDecimal.ZERO else shippingConfig.baseRate

            grandTotal = grandTotal.add(discountedSubtotal).add(tax).add(shipping)
            grandTax = grandTax.add(tax)
        }

        val amountInCents = wompiService.toCents(grandTotal)
        val taxInCents = wompiService.toCents(grandTax)
        val reference = wompiService.generateReference(buyer.id!!)
        val signature = wompiService.computeIntegritySignature(reference, amountInCents, "COP")

        return WompiInitResponse(
            reference = reference,
            amountInCents = amountInCents,
            currency = "COP",
            integritySignature = signature,
            publicKey = wompiService.getPublicKey(),
            taxInCents = taxInCents,
            redirectUrl = "$frontendUrl/checkout",
            customerData = WompiCustomerData(
                email = buyer.email,
                fullName = buyer.name,
                phoneNumber = request.shippingPhone,
            ),
            shippingAddress = WompiShippingAddress(
                addressLine1 = request.shippingAddress,
                city = request.shippingCity,
                country = "CO",
                region = request.shippingState,
                name = buyer.name,
                phoneNumber = request.shippingPhone,
                postalCode = request.shippingPostalCode,
            ),
        )
    }
}
