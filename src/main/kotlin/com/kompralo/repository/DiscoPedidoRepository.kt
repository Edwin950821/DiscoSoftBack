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

    fun findByMeseroId(meseroId: UUID): List<DiscoPedido>

    fun findByMeseroIdAndJornadaFecha(meseroId: UUID, jornadaFecha: String): List<DiscoPedido>

    fun findByEstado(estado: String): List<DiscoPedido>

    fun countByJornadaFecha(jornadaFecha: String): Long

    fun findByMesaIdAndJornadaFechaAndEstado(mesaId: UUID, jornadaFecha: String, estado: String): List<DiscoPedido>

    fun findByMesaIdAndJornadaFechaAndEsCortesiaTrueAndEstadoNot(mesaId: UUID, jornadaFecha: String, estado: String): List<DiscoPedido>

    fun findByMesaIdAndJornadaFechaAndEsCortesiaFalseAndEstado(mesaId: UUID, jornadaFecha: String, estado: String): List<DiscoPedido>

    fun findByCuentaIdAndEstado(cuentaId: UUID, estado: String): List<DiscoPedido>

    fun findByCuentaIdAndEsCortesiaFalseAndEstado(cuentaId: UUID, estado: String): List<DiscoPedido>

    fun findByCuentaIdAndEsCortesiaTrueAndEstadoNot(cuentaId: UUID, estado: String): List<DiscoPedido>

    fun findByCuentaId(cuentaId: UUID): List<DiscoPedido>

    fun findByNegocioIdAndJornadaFechaOrderByCreadoEnDesc(negocioId: UUID, jornadaFecha: String): List<DiscoPedido>

    fun findByNegocioIdAndEstado(negocioId: UUID, estado: String): List<DiscoPedido>

    fun countByNegocioIdAndJornadaFecha(negocioId: UUID, jornadaFecha: String): Long

    fun findByNegocioIdAndMesaIdAndEstado(negocioId: UUID, mesaId: UUID, estado: String): List<DiscoPedido>

    fun findByNegocioIdAndMesaIdAndJornadaFechaAndEstado(negocioId: UUID, mesaId: UUID, jornadaFecha: String, estado: String): List<DiscoPedido>
}
