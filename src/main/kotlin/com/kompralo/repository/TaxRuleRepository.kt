package com.kompralo.repository

import com.kompralo.model.TaxRule
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxRuleRepository : JpaRepository<TaxRule, Long> {
    fun findBySellerOrderByCreatedAtDesc(seller: User): List<TaxRule>
    fun findBySellerAndActive(seller: User, active: Boolean): List<TaxRule>
}
