package com.kompralo.repository

import com.kompralo.model.PolicyType
import com.kompralo.model.StorePolicy
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StorePolicyRepository : JpaRepository<StorePolicy, Long> {
    fun findBySellerOrderByCreatedAtDesc(seller: User): List<StorePolicy>
    fun findBySellerAndPolicyType(seller: User, policyType: PolicyType): StorePolicy?
}
