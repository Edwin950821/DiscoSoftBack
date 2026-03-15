package com.kompralo.port

import com.kompralo.model.Offer
import com.kompralo.model.Order
import com.kompralo.model.Product
import com.kompralo.model.User
import java.math.BigDecimal

interface OfferCalculationPort {

    fun getBestOfferForProduct(productId: Long, quantity: Int, user: User): Offer?

    fun calculateDiscountForProduct(offer: Offer, product: Product, quantity: Int): BigDecimal

    fun canUserUseOffer(offer: Offer, user: User): Boolean

    fun recordUsage(offer: Offer, user: User, order: Order, discountApplied: BigDecimal)
}
