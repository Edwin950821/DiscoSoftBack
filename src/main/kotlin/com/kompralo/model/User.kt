package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entidad Usuario - Representa un usuario en la base de datos
 */
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    var password: String, // Hasheada con BCrypt

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.USER,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Actualiza el timestamp antes de cada update
     */
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

/**
 * Roles de usuario disponibles
 */
enum class Role {
    USER,     // Usuario estándar (comprador)
    BUSINESS, // Negocio/Comercio (vendedor)
    ADMIN     // Administrador
}