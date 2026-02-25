package com.kompralo.repository

import com.kompralo.model.ReturnRequest
import com.kompralo.model.ReturnStatus
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ReturnRequestRepository : JpaRepository<ReturnRequest, Long> {

    fun findByBuyerOrderByCreatedAtDesc(buyer: User): List<ReturnRequest>

    fun findBySellerOrderByCreatedAtDesc(seller: User): List<ReturnRequest>

    fun findBySellerAndStatusOrderByCreatedAtDesc(seller: User, status: ReturnStatus): List<ReturnRequest>

    fun countBySellerAndStatus(seller: User, status: ReturnStatus): Long

    fun countBySeller(seller: User): Long

    fun existsByOrderId(orderId: Long): Boolean

    fun findByOrderId(orderId: Long): ReturnRequest?

    fun findByStatusAndCreatedAtBefore(status: ReturnStatus, date: LocalDateTime): List<ReturnRequest>
}
