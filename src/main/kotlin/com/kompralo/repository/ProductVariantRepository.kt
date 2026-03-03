package com.kompralo.repository

import com.kompralo.model.ProductVariant
import org.springframework.data.jpa.repository.JpaRepository

interface ProductVariantRepository : JpaRepository<ProductVariant, Long> {
    fun findByProductIdAndActiveTrue(productId: Long): List<ProductVariant>
    fun findByProductId(productId: Long): List<ProductVariant>
}
