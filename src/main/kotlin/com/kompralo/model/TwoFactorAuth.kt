package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Entidad para autenticación de dos factores (2FA)
 *
 * Almacena el secreto TOTP y códigos de respaldo para cada usuario
 * Compatible con Google Authenticator y otras apps TOTP
 */
@Entity
@Table(name = "two_factor_auth")
data class TwoFactorAuth(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    val user: User,

    /**
     * Secreto TOTP codificado en Base32
     * Este secreto se usa para generar códigos de 6 dígitos
     */
    @Column(nullable = false)
    val secret: String,

    /**
     * Indica si el 2FA está actualmente habilitado para este usuario
     */
    @Column(nullable = false)
    var enabled: Boolean = false,

    /**
     * Códigos de respaldo de un solo uso
     * Array de strings almacenados en PostgreSQL como TEXT[]
     */
    @Column(columnDefinition = "TEXT[]")
    var backupCodes: Array<String>? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Hook que se ejecuta antes de actualizar la entidad
     */
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    /**
     * Verifica si un código de respaldo es válido
     */
    fun isValidBackupCode(code: String): Boolean {
        return backupCodes?.contains(code) == true
    }

    /**
     * Usa (elimina) un código de respaldo
     */
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
