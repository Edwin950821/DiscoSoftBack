package com.kompralo.repository

import com.kompralo.model.StockBatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface StockBatchRepository : JpaRepository<StockBatch, Long> {
    fun findBySellerIdOrderByCreatedAtDesc(sellerId: Long): List<StockBatch>
    fun findBySellerIdAndCreatedAtAfterOrderByCreatedAtDesc(sellerId: Long, after: LocalDateTime): List<StockBatch>
}
