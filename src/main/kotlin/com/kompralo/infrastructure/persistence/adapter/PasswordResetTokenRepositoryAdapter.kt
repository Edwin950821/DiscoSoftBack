package com.kompralo.infrastructure.persistence.adapter

import com.kompralo.domain.auth.model.PasswordResetToken
import com.kompralo.domain.auth.repository.PasswordResetTokenRepository
import com.kompralo.infrastructure.persistence.mapper.PasswordResetTokenMapper
import com.kompralo.infrastructure.persistence.repository.JpaPasswordResetTokenRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

class PasswordResetTokenRepositoryAdapter(
    private val jpaRepository: JpaPasswordResetTokenRepository,
    private val mapper: PasswordResetTokenMapper
) : PasswordResetTokenRepository {

    override fun findByToken(token: String): PasswordResetToken? {
        return jpaRepository.findByToken(token)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun save(token: PasswordResetToken): PasswordResetToken {
        val entity = mapper.toEntity(token)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun deleteByUserId(userId: Long) {
        jpaRepository.deleteByUserId(userId)
    }

    @Transactional
    override fun deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(LocalDateTime.now())
    }
}
