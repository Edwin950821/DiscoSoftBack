package com.kompralo.repository

import com.kompralo.model.EmailDeliveryLog
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EmailDeliveryLogRepository : JpaRepository<EmailDeliveryLog, Long> {

    fun countByUserIdAndSentAtAfter(userId: Long, since: LocalDateTime): Long

    @Query("""
        SELECT e.offerId, e.offerTitle, e.emailType, COUNT(e), MIN(e.sentAt)
        FROM EmailDeliveryLog e
        WHERE e.offerId IN (SELECT o.id FROM Offer o WHERE o.seller = :seller)
        GROUP BY e.offerId, e.offerTitle, e.emailType
        ORDER BY MIN(e.sentAt) DESC
    """)
    fun findCampaignSummaryBySeller(@Param("seller") seller: User): List<Array<Any>>
}
