package com.kompralo.infrastructure.persistence.mapper

import com.kompralo.domain.auth.model.TwoFactorAuth
import com.kompralo.domain.auth.valueobject.TotpSecret
import com.kompralo.infrastructure.persistence.entity.TwoFactorAuthEntity
import org.springframework.stereotype.Component

@Component
class TwoFactorAuthMapper {

    fun toDomain(entity: TwoFactorAuthEntity): TwoFactorAuth {
        return TwoFactorAuth(
            id = entity.id,
            userId = entity.userId,
            secret = TotpSecret(entity.secret),
            enabled = entity.enabled,
            backupCodes = entity.backupCodes.toList(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: TwoFactorAuth): TwoFactorAuthEntity {
        return TwoFactorAuthEntity(
            id = domain.id,
            userId = domain.userId,
            secret = domain.secret.value,
            enabled = domain.enabled,
            backupCodes = domain.backupCodes.toTypedArray(),
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
