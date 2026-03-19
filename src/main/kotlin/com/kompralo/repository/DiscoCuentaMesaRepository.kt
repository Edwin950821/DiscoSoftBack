package com.kompralo.repository

import com.kompralo.model.DiscoCuentaMesa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoCuentaMesaRepository : JpaRepository<DiscoCuentaMesa, UUID> {

    fun findByMesaIdAndEstado(mesaId: UUID, estado: String): DiscoCuentaMesa?

    fun findByMeseroId(meseroId: UUID): List<DiscoCuentaMesa>

    fun findByMeseroIdAndJornadaFecha(meseroId: UUID, jornadaFecha: String): List<DiscoCuentaMesa>

    fun findByJornadaFechaOrderByCreadoEnDesc(jornadaFecha: String): List<DiscoCuentaMesa>

    fun findByEstado(estado: String): List<DiscoCuentaMesa>
}
