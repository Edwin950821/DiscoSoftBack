package com.kompralo.services

import com.kompralo.config.ShippingConfiguration
import com.kompralo.config.TaxConfiguration
import com.kompralo.dto.CheckoutItemRequest
import com.kompralo.model.Offer
import com.kompralo.model.OfferType
import com.kompralo.model.Product
import com.kompralo.model.User
import com.kompralo.port.OfferCalculationPort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

data class ItemOfferInfo(
    val offer: Offer?,
    val discount: BigDecimal,
    val discountPerUnit: BigDecimal
)

data class SellerPricing(
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val tax: BigDecimal,
    val shipping: BigDecimal,
    val codFee: BigDecimal,
    val total: BigDecimal,
    val hasFreeShipping: Boolean,
    val itemOfferMap: Map<Long, ItemOfferInfo>
)

@Service
class PriceCalculationService(
    private val offerCalculation: OfferCalculationPort,
    private val taxConfig: TaxConfiguration,
    private val shippingConfig: ShippingConfiguration
) {

    fun calculateForSeller(
        sellerItems: List<CheckoutItemRequest>,
        productMap: Map<Long, Product>,
        variantMap: Map<Long, com.kompralo.model.ProductVariant>,
        buyer: User,
        isCod: Boolean
    ): SellerPricing {
        var subtotal = BigDecimal.ZERO
        for (item in sellerItems) {
            val product = productMap[item.productId]!!
            val variant = item.variantId?.let { variantMap[it] }
            val effectivePrice = if (variant != null) product.price.add(variant.priceAdjustment) else product.price
            subtotal = subtotal.add(effectivePrice.multiply(item.quantity.toBigDecimal()))
        }

        var orderDiscount = BigDecimal.ZERO
        var hasFreeShipping = false
        val itemOfferMap = mutableMapOf<Long, ItemOfferInfo>()

        for (item in sellerItems) {
            val product = productMap[item.productId]!!
            try {
                val bestOffer = offerCalculation.getBestOfferForProduct(product.id!!, item.quantity, buyer)
                if (bestOffer != null && offerCalculation.canUserUseOffer(bestOffer, buyer)) {
                    val discount = offerCalculation.calculateDiscountForProduct(bestOffer, product, item.quantity)
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

        val discountedSubtotal = subtotal.subtract(orderDiscount)
        val tax = discountedSubtotal.multiply(taxConfig.ivaRate).setScale(0, RoundingMode.HALF_UP)
        val shipping = if (hasFreeShipping) BigDecimal.ZERO else shippingConfig.baseRate
        val codFee = if (isCod) shippingConfig.codFee else BigDecimal.ZERO
        val total = discountedSubtotal.add(tax).add(shipping).add(codFee)

        return SellerPricing(
            subtotal = subtotal,
            discount = orderDiscount,
            tax = tax,
            shipping = shipping,
            codFee = codFee,
            total = total,
            hasFreeShipping = hasFreeShipping,
            itemOfferMap = itemOfferMap
        )
    }

    fun calculateExpectedTotal(
        itemsBySeller: Map<Long, List<CheckoutItemRequest>>,
        productMap: Map<Long, Product>,
        variantMap: Map<Long, com.kompralo.model.ProductVariant>,
        buyer: User
    ): BigDecimal {
        var expectedTotal = BigDecimal.ZERO
        for ((_, sellerItems) in itemsBySeller) {
            val pricing = calculateForSeller(sellerItems, productMap, variantMap, buyer, false)
            expectedTotal = expectedTotal.add(pricing.total)
        }
        return expectedTotal
    }
}
