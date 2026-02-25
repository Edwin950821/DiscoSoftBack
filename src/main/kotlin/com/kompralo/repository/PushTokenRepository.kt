package com.kompralo.repository

import com.kompralo.model.PushToken
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PushTokenRepository : JpaRepository<PushToken, Long> {
    fun findByUserAndActiveTrue(user: User): List<PushToken>
    fun findByToken(token: String): PushToken?
    fun findByUserAndToken(user: User, token: String): PushToken?
    fun deleteByToken(token: String)

    @Query("SELECT pt FROM PushToken pt WHERE pt.active = true")
    fun findAllActive(): List<PushToken>

    @Modifying
    @Query("UPDATE PushToken pt SET pt.active = false WHERE pt.token = :token")
    fun deactivateByToken(token: String)
}
