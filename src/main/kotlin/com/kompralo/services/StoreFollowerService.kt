package com.kompralo.services

import com.kompralo.model.EmailDeliveryLog
import com.kompralo.model.StoreFollower
import com.kompralo.model.User
import com.kompralo.repository.EmailDeliveryLogRepository
import com.kompralo.repository.OfferRepository
import com.kompralo.repository.StoreFollowerRepository
import com.kompralo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoreFollowerService(
    private val storeFollowerRepository: StoreFollowerRepository,
    private val userRepository: UserRepository,
    private val offerRepository: OfferRepository,
    private val emailService: EmailService,
    private val emailDeliveryLogRepository: EmailDeliveryLogRepository
) {
    private val logger = LoggerFactory.getLogger(StoreFollowerService::class.java)

    @Value("\${app.frontend-url}")
    private lateinit var frontendUrl: String

    @Transactional
    fun followStore(buyer: User, sellerId: Long) {
        val seller = userRepository.findById(sellerId)
            .orElseThrow { RuntimeException("Tienda no encontrada") }

        if (buyer.id == sellerId) {
            throw RuntimeException("No puedes seguir tu propia tienda")
        }

        if (storeFollowerRepository.existsByBuyerAndSeller(buyer, seller)) {
            return
        }

        storeFollowerRepository.save(StoreFollower(buyer = buyer, seller = seller))

        if (!buyer.email.isNullOrBlank()) {
            sendActiveOfferEmailsToNewFollower(buyer, seller)
        }
    }

    @Async
    fun sendActiveOfferEmailsToNewFollower(buyer: User, seller: User) {
        val activeOffers = offerRepository.findActiveEmailCampaignsBySeller(seller)
        if (activeOffers.isEmpty()) return

        val sellerName = seller.name ?: "Tienda"

        for (offer in activeOffers) {
            try {
                val subject = offer.emailSubject ?: "Nueva oferta: ${offer.title}"
                val badge = offer.badgeText ?: ""
                val customMessage = offer.emailMessage ?: ""
                val endDateStr = offer.endDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

                val html = buildWelcomeOfferEmail(offer.title, badge, customMessage, endDateStr, sellerName, offer.imageUrl)

                val sent = emailService.sendHtmlEmailWithAttachment(
                    to = buyer.email!!,
                    subject = subject,
                    htmlContent = html
                )

                emailDeliveryLogRepository.save(
                    EmailDeliveryLog(
                        userId = buyer.id!!,
                        offerId = offer.id!!,
                        offerTitle = offer.title,
                        emailType = "NEW_FOLLOWER",
                        recipientEmail = buyer.email!!,
                        status = if (sent) "SENT" else "FAILED"
                    )
                )

                if (sent) {
                    logger.info("Sent active offer ${offer.id} email to new follower ${buyer.id}")
                }
            } catch (e: Exception) {
                logger.warn("Error sending offer ${offer.id} to new follower ${buyer.id}: ${e.message}")
            }
        }
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun buildWelcomeOfferEmail(title: String, badge: String, message: String, endDate: String, sellerName: String, imageUrl: String?): String {
        val safeTitle = escapeHtml(title)
        val safeBadge = escapeHtml(badge)
        val safeMessage = if (message.isNotBlank()) escapeHtml(message) else ""
        val safeSeller = escapeHtml(sellerName)
        val imageSection = if (!imageUrl.isNullOrBlank()) {
            """<tr><td style="padding:0;"><img src="${escapeHtml(imageUrl)}" alt="$safeTitle" style="width:100%;max-height:250px;object-fit:cover;display:block;" /></td></tr>"""
        } else ""

        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr><td style="background:linear-gradient(135deg,#059669,#10b981);padding:32px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:28px;">KOMPRALO</h1>
                      <p style="color:#d1fae5;margin:8px 0 0;font-size:14px;">Ahora sigues a $safeSeller</p>
                    </td></tr>
                    $imageSection
                    <tr><td style="padding:32px;">
                      <div style="text-align:center;margin-bottom:20px;">
                        <span style="display:inline-block;background:#059669;color:#ffffff;padding:8px 20px;border-radius:20px;font-size:18px;font-weight:bold;">$safeBadge</span>
                      </div>
                      <h2 style="color:#111827;margin:0 0 12px;font-size:22px;text-align:center;">$safeTitle</h2>
                      ${if (safeMessage.isNotBlank()) "<p style=\"color:#374151;margin:0 0 20px;font-size:15px;line-height:1.6;text-align:center;\">$safeMessage</p>" else ""}
                      <table width="100%" cellpadding="0" cellspacing="0" style="background:#f0fdf4;border-radius:12px;margin-bottom:24px;">
                        <tr><td style="padding:16px;text-align:center;">
                          <p style="color:#6b7280;margin:0;font-size:13px;">Valida hasta</p>
                          <p style="color:#059669;margin:4px 0 0;font-size:16px;font-weight:bold;">$endDate</p>
                        </td></tr>
                      </table>
                      <table width="100%" cellpadding="0" cellspacing="0">
                        <tr><td align="center"><a href="$frontendUrl/promociones" style="display:inline-block;background:#059669;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:14px;font-weight:bold;">Ver oferta</a></td></tr>
                      </table>
                    </td></tr>
                    <tr><td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                      <p style="color:#9ca3af;margin:0;font-size:12px;">Recibes este email porque empezaste a seguir a $safeSeller.</p>
                      <p style="color:#9ca3af;margin:4px 0 0;font-size:11px;">Kompralo Marketplace</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
        """.trimIndent()
    }

    @Transactional
    fun unfollowStore(buyer: User, sellerId: Long) {
        val seller = userRepository.findById(sellerId)
            .orElseThrow { RuntimeException("Tienda no encontrada") }

        storeFollowerRepository.deleteByBuyerAndSeller(buyer, seller)
    }

    fun isFollowing(buyer: User, sellerId: Long): Boolean {
        val seller = userRepository.findById(sellerId).orElse(null) ?: return false
        return storeFollowerRepository.existsByBuyerAndSeller(buyer, seller)
    }

    fun getFollowerCount(sellerId: Long): Long {
        val seller = userRepository.findById(sellerId).orElse(null) ?: return 0
        return storeFollowerRepository.countBySeller(seller)
    }
}
