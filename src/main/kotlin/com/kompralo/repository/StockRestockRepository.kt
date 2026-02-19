package com.kompralo.repository

import com.kompralo.model.StockRestock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StockRestockRepository : JpaRepository<StockRestock, Long> {

    fun findByProductIdOrderByCreatedAtDesc(productId: Long): List<StockRestock>
    fun findByBatchIdOrderByCreatedAtDesc(batchId: Long): List<StockRestock>

    @Query("SELECT sr FROM StockRestock sr JOIN FETCH sr.product LEFT JOIN FETCH sr.batch WHERE sr.product.seller.id = :sellerId ORDER BY sr.createdAt DESC")
    fun findBySellerIdWithProduct(@Param("sellerId") sellerId: Long): List<StockRestock>

    @Query("SELECT sr FROM StockRestock sr WHERE sr.product.id = :productId AND sr.quantityRemaining > 0 ORDER BY sr.createdAt ASC")
    fun findByProductIdWithRemainingFIFO(@Param("productId") productId: Long): List<StockRestock>

    @Query(
        "SELECT sr FROM StockRestock sr JOIN FETCH sr.product LEFT JOIN FETCH sr.batch WHERE sr.product.seller.id = :sellerId " +
        "AND (LOWER(sr.product.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(sr.product.sku) LIKE LOWER(CONCAT('%', :q, '%'))) " +
        "ORDER BY sr.createdAt DESC"
    )
    fun searchBySellerAndText(@Param("sellerId") sellerId: Long, @Param("q") q: String): List<StockRestock>
}
