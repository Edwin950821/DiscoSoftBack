package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.OfferRepository
import com.kompralo.repository.OfferUsageRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.SpecialDayRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class OfferService(
    private val offerRepository: OfferRepository,
    private val offerUsageRepository: OfferUsageRepository,
    private val specialDayRepository: SpecialDayRepository,
    private val productRepository: ProductRepository,
    private val offerEmailService: OfferEmailService,
    private val pushNotificationService: PushNotificationService
) {
    private val logger = LoggerFactory.getLogger(OfferService::class.java)

    @Transactional
    fun createOffer(seller: User?, request: CreateOfferRequest): OfferResponse {
        if (request.endDate.isBefore(request.startDate)) {
            throw RuntimeException("La fecha de fin debe ser posterior a la fecha de inicio")
        }
        if (request.type != OfferType.FREE_SHIPPING && request.discountValue <= BigDecimal.ZERO) {
            throw RuntimeException("El valor del descuento debe ser mayor a 0")
        }
        if (request.type == OfferType.PERCENTAGE && request.discountValue > BigDecimal("100")) {
            throw RuntimeException("El porcentaje de descuento no puede ser mayor a 100%")
        }

        val now = LocalDateTime.now()
        val status = when {
            request.startDate.isAfter(now) -> OfferStatus.SCHEDULED
            request.endDate.isAfter(now) -> OfferStatus.ACTIVE
            else -> throw RuntimeException("La oferta ya habría expirado")
        }

        val offer = Offer(
            seller = seller,
            title = request.title,
            description = request.description,
            type = request.type,
            scope = request.scope,
            status = status,
            discountValue = request.discountValue,
            minPurchaseAmount = request.minPurchaseAmount,
            maxDiscountAmount = request.maxDiscountAmount,
            buyQuantity = request.buyQuantity,
            getQuantity = request.getQuantity,
            productIds = request.productIds,
            categoryNames = request.categoryNames,
            startDate = request.startDate,
            endDate = request.endDate,
            maxUses = request.maxUses,
            maxUsesPerUser = request.maxUsesPerUser,
            imageUrl = request.imageUrl,
            badgeText = request.badgeText ?: generateBadgeText(request.type, request.discountValue),
            specialDayId = request.specialDayId,
            emailCampaignEnabled = request.emailCampaignEnabled,
            emailSubject = request.emailSubject,
            emailMessage = request.emailMessage
        )

        val saved = offerRepository.save(offer)

        // If offer is immediately ACTIVE, send push + email now (scheduler only handles SCHEDULED→ACTIVE)
        if (status == OfferStatus.ACTIVE) {
            try {
                pushNotificationService.sendOfferNotification(
                    title = "Nueva oferta: ${saved.title}",
                    body = saved.description ?: "Descuento de ${saved.badgeText ?: saved.discountValue}",
                    offerId = saved.id,
                    imageUrl = saved.imageUrl
                )
            } catch (e: Exception) {
                logger.warn("Error sending push for offer ${saved.id}: ${e.message}")
            }

            if (saved.emailCampaignEnabled && seller != null) {
                try {
                    offerEmailService.sendOfferEmails(saved, seller)
                } catch (e: Exception) {
                    logger.warn("Error sending email campaign for offer ${saved.id}: ${e.message}")
                }
            }
        }

        return saved.toResponse()
    }

    @Transactional
    fun updateOffer(offerId: Long, seller: User?, request: UpdateOfferRequest): OfferResponse {
        val offer = offerRepository.findById(offerId)
            .orElseThrow { RuntimeException("Oferta no encontrada") }

        if (seller != null && offer.seller?.id != seller.id) {
            throw RuntimeException("No tienes permiso para editar esta oferta")
        }

        if (offer.status == OfferStatus.EXPIRED || offer.status == OfferStatus.CANCELLED) {
            throw RuntimeException("No se puede editar una oferta expirada o cancelada")
        }

        request.title?.let { offer.title = it }
        request.description?.let { offer.description = it }
        request.type?.let { offer.type = it }
        request.scope?.let { offer.scope = it }
        request.discountValue?.let { offer.discountValue = it }
        request.minPurchaseAmount?.let { offer.minPurchaseAmount = it }
        request.maxDiscountAmount?.let { offer.maxDiscountAmount = it }
        request.buyQuantity?.let { offer.buyQuantity = it }
        request.getQuantity?.let { offer.getQuantity = it }
        request.productIds?.let { offer.productIds = it }
        request.categoryNames?.let { offer.categoryNames = it }
        request.startDate?.let { offer.startDate = it }
        request.endDate?.let { offer.endDate = it }
        request.maxUses?.let { offer.maxUses = it }
        request.maxUsesPerUser?.let { offer.maxUsesPerUser = it }
        request.imageUrl?.let { offer.imageUrl = it }
        request.badgeText?.let { offer.badgeText = it }
        request.specialDayId?.let { offer.specialDayId = it }
        request.emailCampaignEnabled?.let { offer.emailCampaignEnabled = it }
        request.emailSubject?.let { offer.emailSubject = it }
        request.emailMessage?.let { offer.emailMessage = it }

        return offerRepository.save(offer).toResponse()
    }

    @Transactional
    fun cancelOffer(offerId: Long, seller: User?) {
        val offer = offerRepository.findById(offerId)
            .orElseThrow { RuntimeException("Oferta no encontrada") }

        if (seller != null && offer.seller?.id != seller.id) {
            throw RuntimeException("No tienes permiso para cancelar esta oferta")
        }

        offer.status = OfferStatus.CANCELLED
        offerRepository.save(offer)
    }

    fun getOffersForSeller(seller: User): List<OfferSummaryResponse> {
        return offerRepository.findBySellerOrderByCreatedAtDesc(seller).map { it.toSummary() }
    }

    fun getAllOffers(): List<OfferSummaryResponse> {
        return offerRepository.findAll().sortedByDescending { it.createdAt }.map { it.toSummary() }
    }

    fun getActiveOffers(): List<OfferResponse> {
        return offerRepository.findAllActive(LocalDateTime.now()).map { it.toResponse() }
    }

    fun browseActiveOffers(page: Int, size: Int, type: OfferType?, category: String?): OfferPageResponse {
        val pageable = PageRequest.of(page, size)
        val now = LocalDateTime.now()
        val result = if (type != null) {
            offerRepository.findAllActivePagedByType(now, type, pageable)
        } else {
            offerRepository.findAllActivePaged(now, pageable)
        }
        return OfferPageResponse(
            offers = result.content.map { it.toResponse() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            currentPage = result.number
        )
    }

    fun getUpcomingOffers(): List<OfferResponse> {
        val pageable = PageRequest.of(0, 10)
        return offerRepository.findUpcoming(LocalDateTime.now(), pageable).content.map { it.toResponse() }
    }

    fun getApplicableOffersForProduct(productId: Long): List<ApplicableOfferResponse> {
        val product = productRepository.findById(productId).orElse(null) ?: return emptyList()
        val now = LocalDateTime.now()

        val offers = offerRepository.findActiveForProduct(
            productId = productId,
            category = product.category,
            sellerId = product.seller.id!!,
            now = now
        )

        return offers.filter { it.hasUsesRemaining() }.map { offer ->
            val savedAmount = calculateDiscountForProduct(offer, product, 1)
            ApplicableOfferResponse(
                offerId = offer.id!!,
                title = offer.title,
                type = offer.type,
                badgeText = offer.badgeText,
                discountValue = offer.discountValue,
                savedAmount = savedAmount,
                endDate = offer.endDate
            )
        }.filter { it.savedAmount > BigDecimal.ZERO || it.type == OfferType.FREE_SHIPPING }
    }

    fun getBestOfferForProduct(productId: Long, quantity: Int, user: User): Offer? {
        val product = productRepository.findById(productId).orElse(null) ?: return null
        val now = LocalDateTime.now()

        val offers = offerRepository.findActiveForProduct(
            productId = productId,
            category = product.category,
            sellerId = product.seller.id!!,
            now = now
        )

        return offers
            .filter { it.hasUsesRemaining() && canUserUseOffer(it, user) }
            .maxByOrNull { calculateDiscountForProduct(it, product, quantity) }
    }

    fun calculateDiscountForProduct(offer: Offer, product: Product, quantity: Int): BigDecimal {
        val price = product.price
        return when (offer.type) {
            OfferType.PERCENTAGE -> {
                val discount = price.multiply(offer.discountValue)
                    .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .multiply(quantity.toBigDecimal())
                val maxCap = offer.maxDiscountAmount
                if (maxCap != null && discount > maxCap) maxCap else discount
            }
            OfferType.FIXED_AMOUNT -> {
                val discount = offer.discountValue.multiply(quantity.toBigDecimal())
                val maxTotal = price.multiply(quantity.toBigDecimal())
                if (discount > maxTotal) maxTotal else discount
            }
            OfferType.BUY_X_GET_Y -> {
                val buyQ = offer.buyQuantity ?: return BigDecimal.ZERO
                val getQ = offer.getQuantity ?: return BigDecimal.ZERO
                val groupSize = buyQ + getQ
                if (quantity < groupSize) return BigDecimal.ZERO
                val freeItems = (quantity / groupSize) * getQ
                price.multiply(freeItems.toBigDecimal())
            }
            OfferType.FREE_SHIPPING -> BigDecimal.ZERO
        }
    }

    fun canUserUseOffer(offer: Offer, user: User): Boolean {
        if (!offer.hasUsesRemaining()) return false
        val perUserLimit = offer.maxUsesPerUser ?: return true
        val userUses = offerUsageRepository.countByOfferAndUser(offer, user)
        return userUses < perUserLimit
    }

    @Transactional
    fun recordUsage(offer: Offer, user: User, order: Order, discountApplied: BigDecimal) {
        offerUsageRepository.save(
            OfferUsage(
                offer = offer,
                user = user,
                order = order,
                discountApplied = discountApplied
            )
        )
        offer.currentUses += 1
        offerRepository.save(offer)
    }

    fun getOfferStats(seller: User): OfferStatsResponse {
        val offers = offerRepository.findBySellerOrderByCreatedAtDesc(seller)
        val usages = if (offers.isNotEmpty()) offerUsageRepository.findByOfferIn(offers) else emptyList()

        return OfferStatsResponse(
            totalOffers = offers.size,
            activeOffers = offers.count { it.status == OfferStatus.ACTIVE },
            scheduledOffers = offers.count { it.status == OfferStatus.SCHEDULED },
            expiredOffers = offers.count { it.status == OfferStatus.EXPIRED },
            totalUses = usages.size.toLong(),
            totalDiscountGiven = usages.sumOf { it.discountApplied }
        )
    }

    fun getOfferById(offerId: Long): OfferResponse {
        val offer = offerRepository.findById(offerId)
            .orElseThrow { RuntimeException("Oferta no encontrada") }
        return offer.toResponse()
    }

    // Special Days
    @Transactional
    fun createSpecialDay(seller: User, request: SpecialDayRequest): SpecialDayResponse {
        val day = SpecialDay(
            seller = seller,
            name = request.name,
            date = LocalDate.parse(request.date),
            recurring = request.recurring,
            imageUrl = request.imageUrl,
            description = request.description
        )
        return specialDayRepository.save(day).toResponse()
    }

    @Transactional
    fun updateSpecialDay(id: Long, seller: User, request: SpecialDayRequest): SpecialDayResponse {
        val day = specialDayRepository.findById(id)
            .orElseThrow { RuntimeException("Dia especial no encontrado") }
        if (day.seller?.id != seller.id) {
            throw RuntimeException("No tienes permiso para editar este dia especial")
        }
        day.name = request.name
        day.date = LocalDate.parse(request.date)
        day.recurring = request.recurring
        request.imageUrl?.let { day.imageUrl = it }
        request.description?.let { day.description = it }
        return specialDayRepository.save(day).toResponse()
    }

    @Transactional
    fun deleteSpecialDay(id: Long, seller: User) {
        val day = specialDayRepository.findById(id)
            .orElseThrow { RuntimeException("Dia especial no encontrado") }
        if (day.seller?.id != seller.id) {
            throw RuntimeException("No tienes permiso para eliminar este dia especial")
        }
        specialDayRepository.delete(day)
    }

    fun getSpecialDays(): List<SpecialDayResponse> {
        return specialDayRepository.findByActiveTrue().map { it.toResponse() }
    }

    fun getSpecialDaysForSeller(seller: User): List<SpecialDayResponse> {
        return specialDayRepository.findBySellerOrderByCreatedAtDesc(seller).map { it.toResponse() }
    }

    fun getUpcomingSpecialDays(): List<SpecialDayResponse> {
        val today = LocalDate.now()
        val inThreeMonths = today.plusMonths(3)
        return specialDayRepository.findByDateBetweenAndActiveTrue(today, inThreeMonths)
            .map { it.toResponse() }
    }

    // Mapping helpers
    private fun generateBadgeText(type: OfferType, value: BigDecimal): String {
        return when (type) {
            OfferType.PERCENTAGE -> "-${value.stripTrailingZeros().toPlainString()}%"
            OfferType.FIXED_AMOUNT -> "-\$${value.setScale(0, RoundingMode.HALF_UP)}"
            OfferType.BUY_X_GET_Y -> "2x1"
            OfferType.FREE_SHIPPING -> "Envío gratis"
        }
    }

    private fun Offer.toResponse() = OfferResponse(
        id = id!!,
        sellerId = seller?.id,
        sellerName = seller?.name,
        title = title,
        description = description,
        type = type,
        scope = scope,
        status = status,
        discountValue = discountValue,
        minPurchaseAmount = minPurchaseAmount,
        maxDiscountAmount = maxDiscountAmount,
        buyQuantity = buyQuantity,
        getQuantity = getQuantity,
        productIds = productIds,
        categoryNames = categoryNames,
        startDate = startDate,
        endDate = endDate,
        maxUses = maxUses,
        currentUses = currentUses,
        maxUsesPerUser = maxUsesPerUser,
        imageUrl = imageUrl,
        badgeText = badgeText,
        specialDayId = specialDayId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Offer.toSummary() = OfferSummaryResponse(
        id = id!!,
        title = title,
        type = type,
        scope = scope,
        status = status,
        badgeText = badgeText,
        discountValue = discountValue,
        startDate = startDate,
        endDate = endDate,
        currentUses = currentUses,
        maxUses = maxUses
    )

    private fun SpecialDay.toResponse() = SpecialDayResponse(
        id = id!!,
        name = name,
        date = date.toString(),
        recurring = recurring,
        imageUrl = imageUrl,
        description = description,
        active = active,
        createdAt = createdAt
    )
}
