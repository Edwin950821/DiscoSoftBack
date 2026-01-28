package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entidad para información adicional de perfil de usuario (compradores)
 *
 * Extiende la información básica del User con datos adicionales
 * como dirección, teléfono, avatar, etc.
 */
@Entity
@Table(name = "user_profiles")
data class UserProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    val user: User,

    @Column(length = 20)
    var phone: String? = null,

    @Column(length = 500)
    var avatarUrl: String? = null,

    @Column(columnDefinition = "TEXT")
    var address: String? = null,

    @Column(length = 100)
    var city: String? = null,

    @Column(length = 100)
    var state: String? = null,

    @Column(length = 20)
    var postalCode: String? = null,

    @Column(length = 100)
    var country: String = "Colombia",

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Hook que se ejecuta antes de actualizar la entidad
     * Actualiza automáticamente el timestamp updatedAt
     */
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
