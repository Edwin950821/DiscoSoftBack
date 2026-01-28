package com.kompralo.domain.auth.repository

import com.kompralo.domain.auth.model.PasswordResetToken

interface PasswordResetTokenRepository {
    fun findByToken(token: String): PasswordResetToken?
    fun save(token: PasswordResetToken): PasswordResetToken
    fun deleteByUserId(userId: Long)
    fun deleteExpiredTokens()
}
