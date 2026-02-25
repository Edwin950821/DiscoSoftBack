package com.kompralo.repository

import com.kompralo.model.Order
import com.kompralo.model.OrderStatus
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    fun findByBuyerOrderByCreatedAtDesc(buyer: User): List<Order>

    fun findBySellerOrderByCreatedAtDesc(seller: User): List<Order>

    fun findBySellerAndStatus(seller: User, status: OrderStatus): List<Order>

    fun countBySellerAndStatus(seller: User, status: OrderStatus): Long

    fun findBySellerAndCreatedAtBetween(
        seller: User,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Order>

    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller = :seller AND o.createdAt BETWEEN :startDate AND :endDate")
    fun countOrdersBySellerAndDateRange(
        @Param("seller") seller: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.seller = :seller AND o.createdAt BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID' AND o.status <> 'CANCELLED' AND o.status <> 'REFUNDED'")
    fun sumTotalBySellerAndDateRange(
        @Param("seller") seller: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    @Query("SELECT COALESCE(AVG(o.total), 0) FROM Order o WHERE o.seller = :seller AND o.createdAt BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID' AND o.status <> 'CANCELLED' AND o.status <> 'REFUNDED'")
    fun averageTicketBySellerAndDateRange(
        @Param("seller") seller: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    fun findByOrderNumber(orderNumber: String): Order?

    @Query("""
        SELECT o FROM Order o
        WHERE o.seller = :seller
        AND (LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(o.buyer.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(o.buyer.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY o.createdAt DESC
    """)
    fun searchBySellerAndText(
        @Param("seller") seller: User,
        @Param("search") search: String
    ): List<Order>

    @Query("""
        SELECT o FROM Order o
        WHERE o.seller = :seller
        AND (:status IS NULL OR o.status = :status)
        AND (:startDate IS NULL OR o.createdAt >= :startDate)
        AND (:endDate IS NULL OR o.createdAt <= :endDate)
        ORDER BY o.createdAt DESC
    """)
    fun findBySellerFiltered(
        @Param("seller") seller: User,
        @Param("status") status: OrderStatus?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): List<Order>

    fun countBySeller(seller: User): Long

    @Query("SELECT DISTINCT o.buyer FROM Order o WHERE o.seller = :seller")
    fun findDistinctBuyersBySeller(@Param("seller") seller: User): List<User>

    fun countByBuyerAndSeller(buyer: User, seller: User): Long

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.buyer = :buyer AND o.seller = :seller AND o.status <> 'CANCELLED' AND o.status <> 'REFUNDED'")
    fun sumTotalByBuyerAndSeller(@Param("buyer") buyer: User, @Param("seller") seller: User): BigDecimal

    @Query("SELECT MAX(o.createdAt) FROM Order o WHERE o.buyer = :buyer AND o.seller = :seller")
    fun findLastOrderDateByBuyerAndSeller(@Param("buyer") buyer: User, @Param("seller") seller: User): LocalDateTime?

    @Query("SELECT COUNT(DISTINCT o.buyer) FROM Order o WHERE o.seller = :seller AND o.createdAt >= :since")
    fun countDistinctBuyersBySellerSince(@Param("seller") seller: User, @Param("since") since: LocalDateTime): Long

    @Query("SELECT COUNT(DISTINCT o.buyer) FROM Order o WHERE o.seller = :seller")
    fun countDistinctBuyersBySeller(@Param("seller") seller: User): Long

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.seller = :seller AND o.status = 'REFUNDED' AND o.updatedAt BETWEEN :startDate AND :endDate")
    fun sumRefundedBySellerAndDateRange(
        @Param("seller") seller: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    @Query("SELECT o FROM Order o JOIN FETCH o.buyer JOIN FETCH o.seller LEFT JOIN FETCH o.items WHERE o.id = :id")
    fun findByIdWithDetails(@Param("id") id: Long): Order?

    @Query("SELECT o FROM Order o JOIN FETCH o.buyer JOIN FETCH o.seller LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    fun findByOrderNumberWithDetails(@Param("orderNumber") orderNumber: String): Order?
}
