package com.kompralo.repository

import com.kompralo.model.TwoFactorAuth
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repositorio para autenticación de dos factores
 */
@Repository
interface TwoFactorAuthRepository : JpaRepository<TwoFactorAuth, Long> {

    /**
     * Busca configuración 2FA por usuario
     */
    fun findByUser(user: User): Optional<TwoFactorAuth>

    /**
     * Verifica si un usuario tiene 2FA configurado
     */
    fun existsByUser(user: User): Boolean

    /**
     * Busca configuración 2FA habilitada por usuario
     */
    fun findByUserAndEnabledTrue(user: User): Optional<TwoFactorAuth>

    /**
     * Elimina configuración 2FA de un usuario
     */
    fun deleteByUser(user: User)
}
