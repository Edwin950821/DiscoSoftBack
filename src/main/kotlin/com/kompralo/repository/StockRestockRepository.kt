package com.kompralo.repository

import com.kompralo.model.StockRestock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockRestockRepository : JpaRepository<StockRestock, Long> {

    fun findByProductIdOrderByCreatedAtDesc(productId: Long): List<StockRestock>
    fun findByBatchIdOrderByCreatedAtDesc(batchId: Long): List<StockRestock>
}
