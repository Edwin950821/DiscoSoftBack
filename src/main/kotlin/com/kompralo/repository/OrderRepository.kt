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

/**
 * Repositorio para órdenes
 */
@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    /**
     * Busca órdenes de un comprador
     */
    fun findByBuyerOrderByCreatedAtDesc(buyer: User): List<Order>

    /**
     * Busca órdenes de un vendedor
     */
    fun findBySellerOrderByCreatedAtDesc(seller: User): List<Order>

    /**
     * Busca órdenes de un vendedor por estado
     */
    fun findBySellerAndStatus(seller: User, status: OrderStatus): List<Order>

    /**
     * Cuenta órdenes de un vendedor por estado
     */
    fun countBySellerAndStatus(seller: User, status: OrderStatus): Long

    /**
     * Busca órdenes de un vendedor en un rango de fechas
     */
    fun findBySellerAndCreatedAtBetween(
        seller: User,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Order>

    /**
     * Cuenta órdenes de un vendedor en un rango de fechas
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller = :seller AND o.createdAt BETWEEN :startDate AND :endDate")
    fun countOrdersBySellerAndDateRange(
        @Param("seller") seller: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    /**
     * Suma total de ventas de un vendedor en un rango de fechas (solo órdenes PAGADAS)
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.seller = :seller AND o.createdAt BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID' AND o.status != 'CANCELLED' AND o.status != 'REFUNDED'")
    fun sumTotalBySellerAndDateRange(
        @Param("seller") seller: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    /**
     * Calcula ticket promedio de un vendedor en un rango de fechas (solo órdenes PAGADAS)
     */
    @Query("SELECT COALESCE(AVG(o.total), 0) FROM Order o WHERE o.seller = :seller AND o.createdAt BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID' AND o.status != 'CANCELLED' AND o.status != 'REFUNDED'")
    fun averageTicketBySellerAndDateRange(
        @Param("seller") seller: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    /**
     * Busca orden por número de orden
     */
    fun findByOrderNumber(orderNumber: String): Order?
}
