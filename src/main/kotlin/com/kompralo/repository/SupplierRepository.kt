package com.kompralo.repository

import com.kompralo.model.Supplier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SupplierRepository : JpaRepository<Supplier, Long> {
    fun findBySellerIdAndIsActiveTrueOrderByNameAsc(sellerId: Long): List<Supplier>
    fun findBySellerIdOrderByNameAsc(sellerId: Long): List<Supplier>
    fun existsBySellerIdAndNit(sellerId: Long, nit: String): Boolean
    fun existsBySellerIdAndNitAndIdNot(sellerId: Long, nit: String, id: Long): Boolean
    fun countBySellerIdAndIsActiveTrue(sellerId: Long): Long
    fun countBySellerId(sellerId: Long): Long
}
