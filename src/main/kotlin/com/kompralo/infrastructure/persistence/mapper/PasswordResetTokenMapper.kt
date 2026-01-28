package com.kompralo.infrastructure.persistence.mapper

import com.kompralo.domain.auth.model.PasswordResetToken
import com.kompralo.infrastructure.persistence.entity.PasswordResetTokenEntity
import org.springframework.stereotype.Component

@Component
class PasswordResetTokenMapper {

    fun toDomain(entity: PasswordResetTokenEntity): PasswordResetToken {
        return PasswordResetToken(
            id = entity.id,
            userId = entity.userId,
            token = entity.token,
            expiresAt = entity.expiresAt,
            used = entity.used,
            createdAt = entity.createdAt
        )
    }

    fun toEntity(domain: PasswordResetToken): PasswordResetTokenEntity {
        return PasswordResetTokenEntity(
            id = domain.id,
            userId = domain.userId,
            token = domain.token,
            expiresAt = domain.expiresAt,
            used = domain.used,
            createdAt = domain.createdAt
        )
    }
}
