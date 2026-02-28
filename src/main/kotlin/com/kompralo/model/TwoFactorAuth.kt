package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "two_factor_auth")
data class TwoFactorAuth(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    val user: User,

    @Column(nullable = false)
    val secret: String,

    @Column(nullable = false)
    var enabled: Boolean = false,

    @Column(columnDefinition = "TEXT[]")
    var backupCodes: Array<String>? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun isValidBackupCode(code: String): Boolean {
        return backupCodes?.contains(code) == true
    }

    fun useBackupCode(code: String): Boolean {
        backupCodes?.let { codes ->
            if (codes.contains(code)) {
                backupCodes = codes.filter { it != code }.toTypedArray()
                return true
            }
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TwoFactorAuth) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
