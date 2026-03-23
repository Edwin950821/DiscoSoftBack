package com.kompralo.repository

import com.kompralo.model.DiscoCuentaMesa
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoCuentaMesaRepository : JpaRepository<DiscoCuentaMesa, UUID> {

    fun findByMesaIdAndEstado(mesaId: UUID, estado: String): DiscoCuentaMesa?

    fun findByMeseroId(meseroId: UUID): List<DiscoCuentaMesa>

    fun findByMeseroIdAndJornadaFecha(meseroId: UUID, jornadaFecha: String): List<DiscoCuentaMesa>

    fun findByJornadaFechaOrderByCreadoEnDesc(jornadaFecha: String): List<DiscoCuentaMesa>

    fun findByEstado(estado: String): List<DiscoCuentaMesa>

    fun findByNegocioIdAndJornadaFechaOrderByCreadoEnDesc(negocioId: UUID, jornadaFecha: String): List<DiscoCuentaMesa>

    fun findByNegocioIdAndJornadaFechaAndEstado(negocioId: UUID, jornadaFecha: String, estado: String): List<DiscoCuentaMesa>

    fun findByNegocioIdAndMesaIdAndEstado(negocioId: UUID, mesaId: UUID, estado: String): DiscoCuentaMesa?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DiscoCuentaMesa c WHERE c.negocioId = :negocioId AND c.mesa.id = :mesaId AND c.estado = :estado")
    fun findByNegocioIdAndMesaIdAndEstadoForUpdate(@Param("negocioId") negocioId: UUID, @Param("mesaId") mesaId: UUID, @Param("estado") estado: String): DiscoCuentaMesa?

    @Query("SELECT COUNT(DISTINCT c.mesa.id) FROM DiscoCuentaMesa c WHERE c.negocioId = :negocioId AND c.jornadaFecha = :fecha")
    fun countDistinctMesasByNegocioIdAndJornadaFecha(@Param("negocioId") negocioId: UUID, @Param("fecha") fecha: String): Int
}
