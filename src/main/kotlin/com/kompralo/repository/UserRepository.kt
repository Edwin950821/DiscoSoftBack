package com.kompralo.repository

import com.kompralo.model.Role
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repositorio para operaciones CRUD de usuarios
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {

    /**
     * Busca un usuario por email
     * @return Optional con el usuario si existe
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * Verifica si existe un email en la base de datos
     * @return true si el email ya está registrado
     */
    fun existsByEmail(email: String): Boolean

    /**
     * Busca usuarios por rol
     */
    fun findByRole(role: Role): List<User>

    /**
     * Busca usuarios por múltiples roles
     */
    fun findByRoleIn(roles: List<Role>): List<User>
}