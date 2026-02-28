package com.kompralo.repository

import com.kompralo.model.Offer
import com.kompralo.model.OfferStatus
import com.kompralo.model.OfferType
import com.kompralo.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OfferRepository : JpaRepository<Offer, Long> {

    fun findByStatus(status: OfferStatus): List<Offer>

    fun findByStatusAndStartDateLessThanEqual(status: OfferStatus, now: LocalDateTime): List<Offer>

    fun findByStatusAndEndDateLessThanEqual(status: OfferStatus, now: LocalDateTime): List<Offer>

    fun findBySellerOrderByCreatedAtDesc(seller: User): List<Offer>

    @Query("""
        SELECT o FROM Offer o
        WHERE o.status = 'ACTIVE'
        AND o.startDate <= :now AND o.endDate > :now
        AND (o.scope = 'GLOBAL'
             OR (o.scope = 'PRODUCT' AND o.productIds LIKE CONCAT('%', CAST(:productId AS string), '%'))
             OR (o.scope = 'CATEGORY' AND o.categoryNames LIKE CONCAT('%', :category, '%'))
             OR (o.scope = 'STORE' AND o.seller.id = :sellerId))
    """)
    fun findActiveForProduct(
        @Param("productId") productId: Long,
        @Param("category") category: String,
        @Param("sellerId") sellerId: Long,
        @Param("now") now: LocalDateTime
    ): List<Offer>

    @Query("SELECT o FROM Offer o WHERE o.status = 'ACTIVE' AND o.scope = 'GLOBAL' AND o.startDate <= :now AND o.endDate > :now")
    fun findActiveGlobal(@Param("now") now: LocalDateTime): List<Offer>

    @Query("SELECT o FROM Offer o WHERE o.status = 'ACTIVE' AND o.startDate <= :now AND o.endDate > :now ORDER BY o.createdAt DESC")
    fun findAllActive(@Param("now") now: LocalDateTime): List<Offer>

    @Query("SELECT o FROM Offer o WHERE o.status = 'ACTIVE' AND o.startDate <= :now AND o.endDate > :now ORDER BY o.createdAt DESC")
    fun findAllActivePaged(
        @Param("now") now: LocalDateTime,
        pageable: Pageable
    ): Page<Offer>

    @Query("SELECT o FROM Offer o WHERE o.status = 'ACTIVE' AND o.startDate <= :now AND o.endDate > :now AND o.type = :type ORDER BY o.createdAt DESC")
    fun findAllActivePagedByType(
        @Param("now") now: LocalDateTime,
        @Param("type") type: OfferType,
        pageable: Pageable
    ): Page<Offer>

    @Query("SELECT o FROM Offer o WHERE o.status = 'SCHEDULED' AND o.startDate > :now ORDER BY o.startDate ASC")
    fun findUpcoming(@Param("now") now: LocalDateTime, pageable: Pageable): Page<Offer>

    @Query("SELECT o FROM Offer o WHERE o.seller = :seller AND o.status = 'ACTIVE' AND o.emailCampaignEnabled = true")
    fun findActiveEmailCampaignsBySeller(@Param("seller") seller: User): List<Offer>
}
