package com.kompralo.repository

import com.kompralo.model.SellerProfile
import com.kompralo.model.SellerStatus
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repositorio para perfiles de vendedor
 */
@Repository
interface SellerProfileRepository : JpaRepository<SellerProfile, Long> {

    /**
     * Busca un perfil de vendedor por usuario
     */
    fun findByUser(user: User): Optional<SellerProfile>

    /**
     * Busca un perfil de vendedor por user ID directamente
     */
    fun findByUserId(userId: Long): Optional<SellerProfile>

    /**
     * Verifica si existe un perfil de vendedor para un usuario
     */
    fun existsByUser(user: User): Boolean

    /**
     * Busca vendedores por estado
     */
    fun findByStatus(status: SellerStatus): List<SellerProfile>

    /**
     * Busca vendedores verificados y activos
     */
    fun findByVerifiedTrueAndStatus(status: SellerStatus): List<SellerProfile>
}
