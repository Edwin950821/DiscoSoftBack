package com.kompralo.repository

import com.kompralo.model.SellerProfile
import com.kompralo.model.SellerStatus
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SellerProfileRepository : JpaRepository<SellerProfile, Long> {

    fun findByUser(user: User): Optional<SellerProfile>

    fun findByUserId(userId: Long): Optional<SellerProfile>

    fun existsByUser(user: User): Boolean

    fun findByStatus(status: SellerStatus): List<SellerProfile>

    fun findByVerifiedTrueAndStatus(status: SellerStatus): List<SellerProfile>
}
