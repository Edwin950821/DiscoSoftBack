package com.kompralo.repository

import com.kompralo.model.PasswordResetToken
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repositorio para tokens de recuperación de contraseña
 */
@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {

    /**
     * Busca un token por su valor
     */
    fun findByToken(token: String): Optional<PasswordResetToken>

    /**
     * Busca todos los tokens de un usuario
     */
    fun findByUser(user: User): List<PasswordResetToken>

    /**
     * Elimina todos los tokens de un usuario
     */
    fun deleteByUser(user: User)
}
