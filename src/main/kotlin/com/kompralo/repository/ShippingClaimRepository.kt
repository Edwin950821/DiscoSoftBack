package com.kompralo.repository

import com.kompralo.model.ClaimStatus
import com.kompralo.model.ShippingClaim
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ShippingClaimRepository : JpaRepository<ShippingClaim, Long> {

    fun findByBuyerOrderByCreatedAtDesc(buyer: User): List<ShippingClaim>

    fun findBySellerOrderByCreatedAtDesc(seller: User): List<ShippingClaim>

    fun findBySellerAndStatusOrderByCreatedAtDesc(seller: User, status: ClaimStatus): List<ShippingClaim>

    fun countBySellerAndStatus(seller: User, status: ClaimStatus): Long

    fun countBySeller(seller: User): Long

    fun existsByOrderId(orderId: Long): Boolean

    @Query("SELECT c FROM ShippingClaim c WHERE c.status = :status AND c.storeResponse IS NULL AND c.claimDate < :deadline")
    fun findUnrespondedBefore(
        @Param("status") status: ClaimStatus,
        @Param("deadline") deadline: LocalDateTime
    ): List<ShippingClaim>
}
