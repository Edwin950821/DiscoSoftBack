package com.kompralo.repository

import com.kompralo.model.ProductImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductImageRepository : JpaRepository<ProductImage, Long> {
    fun findByProductIdOrderByPosition(productId: Long): List<ProductImage>
    fun deleteByProductId(productId: Long)
}
