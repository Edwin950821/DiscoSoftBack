package com.kompralo.repository

import com.kompralo.model.Product
import com.kompralo.model.ProductStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : JpaRepository<Product, Long> {

    fun findBySellerId(sellerId: Long): List<Product>

    fun findBySellerIdAndStatus(sellerId: Long, status: ProductStatus): List<Product>

    @Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun searchBySellerAndName(sellerId: Long, search: String): List<Product>

    fun existsBySku(sku: String): Boolean

    fun existsBySkuAndSellerId(sku: String, sellerId: Long): Boolean

    fun findBySku(sku: String): Product?

    fun findByStatus(status: ProductStatus): List<Product>

    fun findByStatusAndCategoryContainingIgnoreCase(status: ProductStatus, category: String): List<Product>

    @Query("SELECT p FROM Product p WHERE p.status = :status AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun searchByStatusAndName(status: ProductStatus, search: String): List<Product>

    @Query("SELECT p FROM Product p JOIN FETCH p.seller WHERE p.id IN :ids")
    fun findAllByIdWithSeller(ids: List<Long>): List<Product>
}
