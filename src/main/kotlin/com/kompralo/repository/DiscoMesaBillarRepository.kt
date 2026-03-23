package com.kompralo.repository

import com.kompralo.model.DiscoMesaBillar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface DiscoMesaBillarRepository : JpaRepository<DiscoMesaBillar, UUID> {
    fun findAllByOrderByNumeroAsc(): List<DiscoMesaBillar>

    fun findByNegocioIdAndActivoTrueOrderByNumeroAsc(negocioId: UUID): List<DiscoMesaBillar>

    @Query("SELECT COALESCE(MAX(m.numero), 0) FROM DiscoMesaBillar m WHERE m.negocioId = :negocioId")
    fun findMaxNumeroByNegocioId(@Param("negocioId") negocioId: UUID): Int
}
