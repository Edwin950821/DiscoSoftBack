package com.kompralo.repository

import com.kompralo.model.InventoryMovement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InventoryMovementRepository : JpaRepository<InventoryMovement, Long> {

    fun findByProductIdOrderByCreatedAtDesc(productId: Long): List<InventoryMovement>

    fun findByRestockIdOrderByCreatedAtDesc(restockId: Long): List<InventoryMovement>

    @Query(
        value = "SELECT m FROM InventoryMovement m JOIN FETCH m.product JOIN FETCH m.user WHERE m.product.seller.id = :sellerId ORDER BY m.createdAt DESC",
        countQuery = "SELECT COUNT(m) FROM InventoryMovement m WHERE m.product.seller.id = :sellerId"
    )
    fun findBySellerIdPaged(
        @Param("sellerId") sellerId: Long,
        pageable: Pageable,
    ): Page<InventoryMovement>

    @Query(
        value = "SELECT m FROM InventoryMovement m JOIN FETCH m.product JOIN FETCH m.user WHERE m.product.seller.id = :sellerId AND m.product.id = :productId ORDER BY m.createdAt DESC",
        countQuery = "SELECT COUNT(m) FROM InventoryMovement m WHERE m.product.seller.id = :sellerId AND m.product.id = :productId"
    )
    fun findBySellerAndProductPaged(
        @Param("sellerId") sellerId: Long,
        @Param("productId") productId: Long,
        pageable: Pageable,
    ): Page<InventoryMovement>
}
