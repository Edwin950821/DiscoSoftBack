package com.kompralo.repository

import com.kompralo.model.AnalyticsEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AnalyticsEventRepository : JpaRepository<AnalyticsEvent, Long> {

    @Query(
        "SELECT COUNT(DISTINCT e.sessionId) FROM AnalyticsEvent e " +
        "WHERE e.sellerId = :sellerId AND e.eventType = :eventType AND e.createdAt >= :since"
    )
    fun countDistinctSessionsBySellerAndType(
        @Param("sellerId") sellerId: Long,
        @Param("eventType") eventType: String,
        @Param("since") since: LocalDateTime,
    ): Long
}
