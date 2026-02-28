package com.kompralo.dto

import com.kompralo.model.OfferScope
import com.kompralo.model.OfferStatus
import com.kompralo.model.OfferType
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateOfferRequest(
    val title: String,
    val description: String? = null,
    val type: OfferType,
    val scope: OfferScope,
    val discountValue: BigDecimal,
    val minPurchaseAmount: BigDecimal? = null,
    val maxDiscountAmount: BigDecimal? = null,
    val buyQuantity: Int? = null,
    val getQuantity: Int? = null,
    val productIds: String? = null,
    val categoryNames: String? = null,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val maxUses: Int? = null,
    val maxUsesPerUser: Int? = null,
    val imageUrl: String? = null,
    val badgeText: String? = null,
    val specialDayId: Long? = null,
    val emailCampaignEnabled: Boolean = false,
    val emailSubject: String? = null,
    val emailMessage: String? = null
)

data class UpdateOfferRequest(
    val title: String? = null,
    val description: String? = null,
    val type: OfferType? = null,
    val scope: OfferScope? = null,
    val discountValue: BigDecimal? = null,
    val minPurchaseAmount: BigDecimal? = null,
    val maxDiscountAmount: BigDecimal? = null,
    val buyQuantity: Int? = null,
    val getQuantity: Int? = null,
    val productIds: String? = null,
    val categoryNames: String? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val maxUses: Int? = null,
    val maxUsesPerUser: Int? = null,
    val imageUrl: String? = null,
    val badgeText: String? = null,
    val specialDayId: Long? = null,
    val emailCampaignEnabled: Boolean? = null,
    val emailSubject: String? = null,
    val emailMessage: String? = null
)

data class OfferResponse(
    val id: Long,
    val sellerId: Long?,
    val sellerName: String?,
    val title: String,
    val description: String?,
    val type: OfferType,
    val scope: OfferScope,
    val status: OfferStatus,
    val discountValue: BigDecimal,
    val minPurchaseAmount: BigDecimal?,
    val maxDiscountAmount: BigDecimal?,
    val buyQuantity: Int?,
    val getQuantity: Int?,
    val productIds: String?,
    val categoryNames: String?,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val maxUses: Int?,
    val currentUses: Int,
    val maxUsesPerUser: Int?,
    val imageUrl: String?,
    val badgeText: String?,
    val specialDayId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class OfferSummaryResponse(
    val id: Long,
    val title: String,
    val type: OfferType,
    val scope: OfferScope,
    val status: OfferStatus,
    val badgeText: String?,
    val discountValue: BigDecimal,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val currentUses: Int,
    val maxUses: Int?
)

data class ApplicableOfferResponse(
    val offerId: Long,
    val title: String,
    val type: OfferType,
    val badgeText: String?,
    val discountValue: BigDecimal,
    val savedAmount: BigDecimal,
    val endDate: LocalDateTime
)

data class OfferStatsResponse(
    val totalOffers: Int,
    val activeOffers: Int,
    val scheduledOffers: Int,
    val expiredOffers: Int,
    val totalUses: Long,
    val totalDiscountGiven: BigDecimal
)

data class OfferPageResponse(
    val offers: List<OfferResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

data class EmailCampaignSummary(
    val offerId: Long,
    val offerTitle: String,
    val emailType: String,
    val totalSent: Long,
    val sentAt: LocalDateTime
)

data class SpecialDayRequest(
    val name: String,
    val date: String,
    val recurring: Boolean = false,
    val imageUrl: String? = null,
    val description: String? = null
)

data class SpecialDayResponse(
    val id: Long,
    val name: String,
    val date: String,
    val recurring: Boolean,
    val imageUrl: String?,
    val description: String?,
    val active: Boolean,
    val createdAt: LocalDateTime
)
