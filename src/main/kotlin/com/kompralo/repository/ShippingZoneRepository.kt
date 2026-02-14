package com.kompralo.repository

import com.kompralo.model.ShippingZone
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShippingZoneRepository : JpaRepository<ShippingZone, Long> {
    fun findBySellerOrderByCreatedAtDesc(seller: User): List<ShippingZone>
    fun findBySellerAndActive(seller: User, active: Boolean): List<ShippingZone>
}
