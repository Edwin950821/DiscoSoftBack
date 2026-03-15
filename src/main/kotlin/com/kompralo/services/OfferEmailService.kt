package com.kompralo.services

import com.kompralo.dto.EmailCampaignSummary
import com.kompralo.model.EmailDeliveryLog
import com.kompralo.model.Offer
import com.kompralo.model.User
import com.kompralo.repository.EmailDeliveryLogRepository
import com.kompralo.repository.OrderRepository
import com.kompralo.port.EmailPort
import com.kompralo.repository.StoreFollowerRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OfferEmailService(
    private val emailPort: EmailPort,
    private val orderRepository: OrderRepository,
    private val storeFollowerRepository: StoreFollowerRepository,
    private val emailDeliveryLogRepository: EmailDeliveryLogRepository
) {
    private val logger = LoggerFactory.getLogger(OfferEmailService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    @Value("\${app.frontend-url}")
    private lateinit var frontendUrl: String

    private val MAX_EMAILS_PER_USER_PER_WEEK = 3

    @Async
    fun sendOfferEmails(offer: Offer, seller: User) {
        if (!offer.emailCampaignEnabled) return

        val recipients = resolveRecipients(seller)
        if (recipients.isEmpty()) {
            logger.info("No recipients found for offer ${offer.id} email campaign")
            return
        }

        val subject = offer.emailSubject ?: "Nueva oferta: ${offer.title}"
        val html = buildOfferEmailHtml(offer, seller)
        var sentCount = 0
        var skippedCount = 0

        for (recipient in recipients) {
            if (recipient.email.isNullOrBlank()) continue

            val recentCount = emailDeliveryLogRepository.countByUserIdAndSentAtAfter(
                recipient.id!!, LocalDateTime.now().minusDays(7)
            )
            if (recentCount >= MAX_EMAILS_PER_USER_PER_WEEK) {
                skippedCount++
                continue
            }

            try {
                val sent = emailPort.sendHtmlEmailWithAttachment(
                    to = recipient.email!!,
                    subject = subject,
                    htmlContent = html
                )

                emailDeliveryLogRepository.save(
                    EmailDeliveryLog(
                        userId = recipient.id!!,
                        offerId = offer.id!!,
                        offerTitle = offer.title,
                        emailType = "OFFER_LIVE",
                        recipientEmail = recipient.email!!,
                        status = if (sent) "SENT" else "FAILED"
                    )
                )

                if (sent) sentCount++
            } catch (e: Exception) {
                logger.warn("Error sending offer email to ${recipient.email}: ${e.message}")
                emailDeliveryLogRepository.save(
                    EmailDeliveryLog(
                        userId = recipient.id!!,
                        offerId = offer.id!!,
                        offerTitle = offer.title,
                        emailType = "OFFER_LIVE",
                        recipientEmail = recipient.email!!,
                        status = "FAILED"
                    )
                )
            }
        }

        logger.info("Offer ${offer.id} email campaign: sent=$sentCount, skipped=$skippedCount, total=${recipients.size}")
    }

    private fun resolveRecipients(seller: User): List<User> {
        val buyers = orderRepository.findDistinctBuyersBySeller(seller)
        val followers = storeFollowerRepository.findFollowersBySeller(seller)

        val allRecipients = mutableMapOf<Long, User>()
        buyers.forEach { allRecipients[it.id!!] = it }
        followers.forEach { allRecipients[it.id!!] = it }

        return allRecipients.values.toList()
    }

    fun getCampaignHistory(seller: User): List<EmailCampaignSummary> {
        return emailDeliveryLogRepository.findCampaignSummaryBySeller(seller).map { row ->
            EmailCampaignSummary(
                offerId = row[0] as Long,
                offerTitle = row[1] as String,
                emailType = row[2] as String,
                totalSent = row[3] as Long,
                sentAt = row[4] as LocalDateTime
            )
        }
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun buildOfferEmailHtml(offer: Offer, seller: User): String {
        val sellerName = escapeHtml(seller.name ?: "Tienda")
        val offerTitle = escapeHtml(offer.title)
        val badge = escapeHtml(offer.badgeText ?: "")
        val customMessage = offer.emailMessage?.let { escapeHtml(it) } ?: ""
        val endDateStr = offer.endDate.format(dateFormatter)
        val imageSection = if (!offer.imageUrl.isNullOrBlank()) {
            """
            <tr>
              <td style="padding:0;">
                <img src="${escapeHtml(offer.imageUrl!!)}" alt="$offerTitle" style="width:100%;max-height:250px;object-fit:cover;display:block;" />
              </td>
            </tr>
            """
        } else ""

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#059669,#10b981);padding:32px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;">KOMPRALO</h1>
                        <p style="color:#d1fae5;margin:8px 0 0;font-size:14px;">Oferta Especial de $sellerName</p>
                      </td>
                    </tr>
                    $imageSection
                    <tr>
                      <td style="padding:32px;">
                        <div style="text-align:center;margin-bottom:20px;">
                          <span style="display:inline-block;background:#059669;color:#ffffff;padding:8px 20px;border-radius:20px;font-size:18px;font-weight:bold;">$badge</span>
                        </div>
                        <h2 style="color:#111827;margin:0 0 12px;font-size:22px;text-align:center;">$offerTitle</h2>
                        ${if (customMessage.isNotBlank()) """
                        <p style="color:#374151;margin:0 0 20px;font-size:15px;line-height:1.6;text-align:center;">$customMessage</p>
                        """ else ""}
                        <table width="100%" cellpadding="0" cellspacing="0" style="background:#f0fdf4;border-radius:12px;margin-bottom:24px;">
                          <tr><td style="padding:16px;text-align:center;">
                            <p style="color:#6b7280;margin:0;font-size:13px;">Valida hasta</p>
                            <p style="color:#059669;margin:4px 0 0;font-size:16px;font-weight:bold;">$endDateStr</p>
                          </td></tr>
                        </table>
                        <table width="100%" cellpadding="0" cellspacing="0">
                          <tr><td align="center">
                            <a href="$frontendUrl/promociones" style="display:inline-block;background:#059669;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:14px;font-weight:bold;">
                              Ver oferta
                            </a>
                          </td></tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                        <p style="color:#9ca3af;margin:0;font-size:12px;">Recibes este email porque compraste en $sellerName o sigues esta tienda.</p>
                        <p style="color:#9ca3af;margin:4px 0 0;font-size:11px;">Kompralo Marketplace</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }
}
