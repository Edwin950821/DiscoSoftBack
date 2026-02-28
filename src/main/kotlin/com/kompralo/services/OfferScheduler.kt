package com.kompralo.services

import com.kompralo.model.NotificationType
import com.kompralo.model.OfferStatus
import com.kompralo.model.RelatedEntityType
import com.kompralo.repository.OfferRepository
import com.kompralo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class OfferScheduler(
    private val offerRepository: OfferRepository,
    private val pushNotificationService: PushNotificationService,
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
    private val offerEmailService: OfferEmailService
) {
    private val logger = LoggerFactory.getLogger(OfferScheduler::class.java)

    @Scheduled(fixedRate = 60000)
    @Transactional
    fun activateScheduledOffers() {
        val now = LocalDateTime.now()
        val toActivate = offerRepository.findByStatusAndStartDateLessThanEqual(OfferStatus.SCHEDULED, now)

        for (offer in toActivate) {
            offer.status = OfferStatus.ACTIVE
            offerRepository.save(offer)
            logger.info("Offer activated: ${offer.id} - ${offer.title}")

            // Push notification to all buyers
            try {
                pushNotificationService.sendOfferNotification(
                    title = "Nueva oferta: ${offer.title}",
                    body = offer.description ?: "Descuento de ${offer.badgeText ?: offer.discountValue}",
                    offerId = offer.id,
                    imageUrl = offer.imageUrl
                )
            } catch (e: Exception) {
                logger.warn("Error sending push for offer ${offer.id}: ${e.message}")
            }

            if (offer.seller != null) {
                try {
                    notificationService.createAndSend(
                        userId = offer.seller!!.id!!,
                        type = NotificationType.PROMO_OFFER,
                        title = "Tu oferta se ha activado",
                        message = "La oferta '${offer.title}' está activa hasta ${offer.endDate}.",
                        priority = "medium",
                        actionUrl = "/admin/marketing",
                        relatedEntityId = offer.id,
                        relatedEntityType = RelatedEntityType.OFFER
                    )
                } catch (e: Exception) {
                    logger.warn("Error sending activation notification: ${e.message}")
                }
            }

            if (offer.emailCampaignEnabled && offer.seller != null) {
                try {
                    offerEmailService.sendOfferEmails(offer, offer.seller!!)
                } catch (e: Exception) {
                    logger.warn("Error sending email campaign for offer ${offer.id}: ${e.message}")
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    fun expireActiveOffers() {
        val now = LocalDateTime.now()
        val toExpire = offerRepository.findByStatusAndEndDateLessThanEqual(OfferStatus.ACTIVE, now)

        for (offer in toExpire) {
            offer.status = OfferStatus.EXPIRED
            offerRepository.save(offer)
            logger.info("Offer expired: ${offer.id} - ${offer.title}")

            if (offer.seller != null) {
                try {
                    notificationService.createAndSend(
                        userId = offer.seller!!.id!!,
                        type = NotificationType.PROMO_OFFER,
                        title = "Oferta expirada",
                        message = "La oferta '${offer.title}' ha finalizado. Usos totales: ${offer.currentUses}.",
                        priority = "low",
                        actionUrl = "/admin/marketing",
                        relatedEntityId = offer.id,
                        relatedEntityType = RelatedEntityType.OFFER
                    )
                } catch (e: Exception) {
                    logger.warn("Error sending expiration notification: ${e.message}")
                }
            }
        }
    }
}
