package com.kompralo.infrastructure.persistence.repository

import com.kompralo.infrastructure.persistence.entity.TwoFactorAuthEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface JpaTwoFactorAuthRepository : JpaRepository<TwoFactorAuthEntity, Long> {
    fun findByUserId(userId: Long): Optional<TwoFactorAuthEntity>
    fun findByUserIdAndEnabled(userId: Long, enabled: Boolean): Optional<TwoFactorAuthEntity>
    fun deleteByUserId(userId: Long)
}
