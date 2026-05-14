package com.kompralo.infrastructure.persistence.adapter

import com.kompralo.domain.auth.model.TwoFactorAuth
import com.kompralo.domain.auth.repository.TwoFactorAuthRepository
import com.kompralo.infrastructure.persistence.mapper.TwoFactorAuthMapper
import com.kompralo.infrastructure.persistence.repository.JpaTwoFactorAuthRepository
import org.springframework.transaction.annotation.Transactional

class TwoFactorAuthRepositoryAdapter(
    private val jpaRepository: JpaTwoFactorAuthRepository,
    private val mapper: TwoFactorAuthMapper
) : TwoFactorAuthRepository {

    override fun findByUserId(userId: Long): TwoFactorAuth? {
        return jpaRepository.findByUserId(userId)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByUserIdAndEnabled(userId: Long, enabled: Boolean): TwoFactorAuth? {
        return jpaRepository.findByUserIdAndEnabled(userId, enabled)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun save(twoFactorAuth: TwoFactorAuth): TwoFactorAuth {
        val entity = mapper.toEntity(twoFactorAuth)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun deleteByUserId(userId: Long) {
        jpaRepository.deleteByUserId(userId)
    }
}
