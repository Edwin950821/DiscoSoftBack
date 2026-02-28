package com.kompralo.repository

import com.kompralo.model.StoreFollower
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StoreFollowerRepository : JpaRepository<StoreFollower, Long> {

    fun findByBuyerAndSeller(buyer: User, seller: User): StoreFollower?

    fun existsByBuyerAndSeller(buyer: User, seller: User): Boolean

    fun deleteByBuyerAndSeller(buyer: User, seller: User)

    @Query("SELECT sf.buyer FROM StoreFollower sf WHERE sf.seller = :seller")
    fun findFollowersBySeller(@Param("seller") seller: User): List<User>

    fun countBySeller(seller: User): Long
}
