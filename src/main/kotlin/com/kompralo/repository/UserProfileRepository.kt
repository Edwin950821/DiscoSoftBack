package com.kompralo.repository

import com.kompralo.model.User
import com.kompralo.model.UserProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repositorio para perfiles de usuario
 */
@Repository
interface UserProfileRepository : JpaRepository<UserProfile, Long> {

    /**
     * Busca un perfil por usuario
     */
    fun findByUser(user: User): Optional<UserProfile>

    /**
     * Verifica si existe un perfil para un usuario
     */
    fun existsByUser(user: User): Boolean
}
