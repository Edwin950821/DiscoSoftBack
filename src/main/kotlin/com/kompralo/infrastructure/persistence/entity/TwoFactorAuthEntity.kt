package com.kompralo.infrastructure.persistence.entity

import java.time.LocalDateTime

/**
 * DTO de persistencia (NO es @Entity para evitar conflicto con com.kompralo.model.TwoFactorAuth)
 * La entidad JPA principal es com.kompralo.model.TwoFactorAuth
 */
data class TwoFactorAuthEntity(
    val id: Long? = null,
    val userId: Long,
    val secret: String,
    var enabled: Boolean,
    var backupCodes: Array<String>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TwoFactorAuthEntity

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (secret != other.secret) return false
        if (enabled != other.enabled) return false
        if (!backupCodes.contentEquals(other.backupCodes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + userId.hashCode()
        result = 31 * result + secret.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + backupCodes.contentHashCode()
        return result
    }
}
