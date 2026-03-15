package com.kompralo.repository

import com.kompralo.model.DiscoPedido
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoPedidoRepository : JpaRepository<DiscoPedido, UUID> {

    fun findAllByOrderByCreadoEnDesc(): List<DiscoPedido>

    fun findByJornadaFechaOrderByCreadoEnDesc(jornadaFecha: String): List<DiscoPedido>

    fun findByMesaIdAndEstado(mesaId: UUID, estado: String): List<DiscoPedido>

    fun findByMeseroIdAndJornadaFecha(meseroId: UUID, jornadaFecha: String): List<DiscoPedido>

    fun findByEstado(estado: String): List<DiscoPedido>

    fun countByJornadaFecha(jornadaFecha: String): Long

    fun findByMesaIdAndJornadaFechaAndEstado(mesaId: UUID, jornadaFecha: String, estado: String): List<DiscoPedido>
}
