package com.kompralo.repository

import com.kompralo.model.DiscoPartidaBillar
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DiscoPartidaBillarRepository : JpaRepository<DiscoPartidaBillar, UUID> {
    fun findByMesaBillarIdAndEstado(mesaBillarId: UUID, estado: String): DiscoPartidaBillar?
    fun findByJornadaFechaOrderByCreadoEnDesc(jornadaFecha: String): List<DiscoPartidaBillar>
    fun findByJornadaFechaAndEstado(jornadaFecha: String, estado: String): List<DiscoPartidaBillar>

    fun findByNegocioIdAndJornadaFechaOrderByCreadoEnDesc(negocioId: UUID, jornadaFecha: String): List<DiscoPartidaBillar>

    fun findByNegocioIdAndJornadaFechaAndEstado(negocioId: UUID, jornadaFecha: String, estado: String): List<DiscoPartidaBillar>
}
