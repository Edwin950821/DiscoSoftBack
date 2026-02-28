package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = [
        Index(name = "idx_password_reset_token", columnList = "token"),
        Index(name = "idx_password_reset_user", columnList = "user_id")
    ]
)
data class PasswordResetToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(unique = true, nullable = false)
    val token: String,

    @Column(nullable = false)
    val expiresAt: LocalDateTime,

    @Column(nullable = false)
    var used: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }

    fun isValid(): Boolean {
        return !used && !isExpired()
    }
}
