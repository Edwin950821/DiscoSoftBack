package com.kompralo.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "two_factor_auth")
data class TwoFactorAuthEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", unique = true, nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val secret: String,

    @Column(nullable = false)
    var enabled: Boolean,

    @Column(nullable = false, columnDefinition = "TEXT[]")
    var backupCodes: Array<String>,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

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
