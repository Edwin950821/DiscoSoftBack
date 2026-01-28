package com.kompralo.domain.auth.repository

import com.kompralo.domain.auth.model.TwoFactorAuth

interface TwoFactorAuthRepository {
    fun findByUserId(userId: Long): TwoFactorAuth?
    fun findByUserIdAndEnabled(userId: Long, enabled: Boolean): TwoFactorAuth?
    fun save(twoFactorAuth: TwoFactorAuth): TwoFactorAuth
    fun deleteByUserId(userId: Long)
}
