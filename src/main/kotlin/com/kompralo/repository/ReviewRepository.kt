package com.kompralo.repository

import com.kompralo.model.Review
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByProductIdOrderByCreatedAtDesc(productId: Long): List<Review>
    fun existsByProductIdAndBuyerId(productId: Long, buyerId: Long): Boolean

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    fun averageRatingByProductId(productId: Long): Double?

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    fun countByProductId(productId: Long): Long
}
