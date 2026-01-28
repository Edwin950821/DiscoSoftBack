package com.kompralo.infrastructure.persistence.repository

import com.kompralo.infrastructure.persistence.entity.PasswordResetTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.*

interface JpaPasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, Long> {
    fun findByToken(token: String): Optional<PasswordResetTokenEntity>
    fun deleteByUserId(userId: Long)

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity p WHERE p.expiresAt < :now")
    fun deleteExpiredTokens(now: LocalDateTime)
}
