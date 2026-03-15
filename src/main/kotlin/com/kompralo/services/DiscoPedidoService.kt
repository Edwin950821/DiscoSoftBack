package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class DiscoPedidoService(
    private val pedidoRepo: DiscoPedidoRepository,
    private val mesaRepo: DiscoMesaRepository,
    private val meseroRepo: DiscoMeseroRepository,
    private val productoRepo: DiscoProductoRepository,
    private val cuentaRepo: DiscoCuentaMesaRepository,
    private val socketIO: SocketIOService
) {

    private val hoy: String get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    @Transactional
    fun atenderMesa(mesaId: UUID, req: DiscoAtenderMesaRequest): DiscoMesaResponse {
        val mesa = mesaRepo.findById(mesaId)
            .orElseThrow { RuntimeException("Mesa no encontrada") }
        val mesero = meseroRepo.findById(req.meseroId)
            .orElseThrow { RuntimeException("Mesero no encontrado") }

        if (mesa.estado == "OCUPADA") {
            throw IllegalStateException("La mesa ya está siendo atendida por otro mesero")
        }

        mesa.estado = "OCUPADA"
        mesa.mesero = mesero
        mesa.nombreCliente = req.nombreCliente

        val cuentaExistente = cuentaRepo.findByMesaIdAndEstado(mesaId, "ABIERTA")
        if (cuentaExistente == null) {
            val cuenta = DiscoCuentaMesa(
                mesa = mesa,
                mesero = mesero,
                nombreCliente = req.nombreCliente,
                jornadaFecha = hoy
            )
            cuentaRepo.save(cuenta)
        }

        val response = mesaRepo.save(mesa).toResponse()
        socketIO.sendToAdmin("mesa_ocupada", response)
        socketIO.sendToAllMeseros("mesa_actualizada", response)
        return response
    }

    @Transactional
    fun liberarMesa(mesaId: UUID) {
        val mesa = mesaRepo.findById(mesaId)
            .orElseThrow { RuntimeException("Mesa no encontrada") }
        mesa.estado = "LIBRE"
        mesa.mesero = null
        mesa.nombreCliente = null
        mesaRepo.save(mesa)
    }

    @Transactional
    fun crearPedido(req: DiscoPedidoRequest): DiscoPedidoResponse {
        val mesa = mesaRepo.findById(req.mesaId)
            .orElseThrow { RuntimeException("Mesa no encontrada") }
        val mesero = meseroRepo.findById(req.meseroId)
            .orElseThrow { RuntimeException("Mesero no encontrado") }

        if (mesa.estado != "OCUPADA" || mesa.mesero?.id != mesero.id) {
            throw IllegalStateException("Este mesero no está atendiendo esta mesa")
        }

        val ticketDia = (pedidoRepo.countByJornadaFecha(hoy) + 1).toInt()

        val pedido = DiscoPedido(
            mesa = mesa,
            mesero = mesero,
            ticketDia = ticketDia,
            jornadaFecha = hoy,
            nota = req.nota
        )

        var totalPedido = 0
        req.lineas.forEach { lReq ->
            val producto = productoRepo.findById(lReq.productoId)
                .orElseThrow { RuntimeException("Producto no encontrado: ${lReq.productoId}") }
            val lineaTotal = producto.precio * lReq.cantidad
            totalPedido += lineaTotal

            val linea = DiscoLineaPedido(
                producto = producto,
                nombre = producto.nombre,
                precioUnitario = producto.precio,
                cantidad = lReq.cantidad,
                total = lineaTotal
            )
            linea.pedido = pedido
            pedido.lineas.add(linea)
        }

        pedido.total = totalPedido
        val response = pedidoRepo.save(pedido).toResponse()
        socketIO.sendToAdmin("nuevo_pedido", response)
        return response
    }

    @Transactional
    fun despacharPedido(pedidoId: UUID): DiscoPedidoResponse {
        val pedido = pedidoRepo.findById(pedidoId)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (pedido.estado != "PENDIENTE") {
            throw IllegalStateException("El pedido no está pendiente, estado actual: ${pedido.estado}")
        }

        pedido.estado = "DESPACHADO"
        pedido.despachadoEn = LocalDateTime.now()

        val cuenta = cuentaRepo.findByMesaIdAndEstado(pedido.mesa.id!!, "ABIERTA")
            ?: throw RuntimeException("No hay cuenta abierta para esta mesa")
        cuenta.total += pedido.total
        cuentaRepo.save(cuenta)

        val response = pedidoRepo.save(pedido).toResponse()
        socketIO.sendToMesero(pedido.mesero.id.toString(), "pedido_despachado", response)
        socketIO.sendToAdmin("pedido_despachado", response)
        return response
    }

    @Transactional
    fun cancelarPedido(pedidoId: UUID): DiscoPedidoResponse {
        val pedido = pedidoRepo.findById(pedidoId)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (pedido.estado == "CANCELADO") {
            throw IllegalStateException("El pedido ya está cancelado")
        }

        if (pedido.estado == "DESPACHADO") {
            val cuenta = cuentaRepo.findByMesaIdAndEstado(pedido.mesa.id!!, "ABIERTA")
            if (cuenta != null) {
                cuenta.total -= pedido.total
                if (cuenta.total < 0) cuenta.total = 0
                cuentaRepo.save(cuenta)
            }
        }

        pedido.estado = "CANCELADO"
        pedido.canceladoEn = LocalDateTime.now()
        val response = pedidoRepo.save(pedido).toResponse()
        socketIO.sendToMesero(pedido.mesero.id.toString(), "pedido_cancelado", response)
        socketIO.sendToAdmin("pedido_cancelado", response)
        return response
    }

    @Transactional
    fun editarPedido(pedidoId: UUID, req: DiscoPedidoRequest): DiscoPedidoResponse {
        val pedido = pedidoRepo.findById(pedidoId)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (pedido.estado == "CANCELADO") {
            throw IllegalStateException("No se puede editar un pedido cancelado")
        }

        val totalAnterior = if (pedido.estado == "DESPACHADO") pedido.total else 0

        pedido.lineas.clear()
        var nuevoTotal = 0
        req.lineas.forEach { lReq ->
            val producto = productoRepo.findById(lReq.productoId)
                .orElseThrow { RuntimeException("Producto no encontrado: ${lReq.productoId}") }
            val lineaTotal = producto.precio * lReq.cantidad
            nuevoTotal += lineaTotal

            val linea = DiscoLineaPedido(
                producto = producto,
                nombre = producto.nombre,
                precioUnitario = producto.precio,
                cantidad = lReq.cantidad,
                total = lineaTotal
            )
            linea.pedido = pedido
            pedido.lineas.add(linea)
        }

        pedido.total = nuevoTotal

        if (pedido.estado == "DESPACHADO") {
            val cuenta = cuentaRepo.findByMesaIdAndEstado(pedido.mesa.id!!, "ABIERTA")
            if (cuenta != null) {
                cuenta.total = cuenta.total - totalAnterior + nuevoTotal
                cuentaRepo.save(cuenta)
            }
        }

        return pedidoRepo.save(pedido).toResponse()
    }

    @Transactional
    fun pagarCuenta(mesaId: UUID): DiscoCuentaMesaResponse {
        val cuenta = cuentaRepo.findByMesaIdAndEstado(mesaId, "ABIERTA")
            ?: throw RuntimeException("No hay cuenta abierta para esta mesa")

        cuenta.estado = "PAGADA"
        cuenta.pagadaEn = LocalDateTime.now()
        cuentaRepo.save(cuenta)

        liberarMesa(mesaId)

        val pedidos = pedidoRepo.findByMesaIdAndJornadaFechaAndEstado(
            mesaId, cuenta.jornadaFecha, "DESPACHADO"
        )

        val response = cuenta.toResponse(pedidos)
        socketIO.sendToMesero(cuenta.mesero.id.toString(), "cuenta_pagada", response)
        socketIO.sendToAdmin("cuenta_pagada", response)
        socketIO.sendToAllMeseros("mesa_actualizada", DiscoMesaResponse(
            id = cuenta.mesa.id!!,
            numero = cuenta.mesa.numero,
            nombre = cuenta.mesa.nombre,
            estado = "LIBRE"
        ))
        return response
    }

    @Transactional(readOnly = true)
    fun getPedidosHoy(): List<DiscoPedidoResponse> =
        pedidoRepo.findByJornadaFechaOrderByCreadoEnDesc(hoy).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getPedidosPendientes(): List<DiscoPedidoResponse> =
        pedidoRepo.findByEstado("PENDIENTE").map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getPedidosPorMesa(mesaId: UUID): List<DiscoPedidoResponse> =
        pedidoRepo.findByMesaIdAndEstado(mesaId, "DESPACHADO").map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getCuentaMesa(mesaId: UUID): DiscoCuentaMesaResponse? {
        val cuenta = cuentaRepo.findByMesaIdAndEstado(mesaId, "ABIERTA") ?: return null
        val pedidos = pedidoRepo.findByMesaIdAndJornadaFechaAndEstado(
            mesaId, cuenta.jornadaFecha, "DESPACHADO"
        )
        return cuenta.toResponse(pedidos)
    }

    @Transactional(readOnly = true)
    fun getCuentasHoy(): List<DiscoCuentaMesaResponse> {
        val cuentas = cuentaRepo.findByJornadaFechaOrderByCreadoEnDesc(hoy)
        return cuentas.map { cuenta ->
            val pedidos = pedidoRepo.findByMesaIdAndJornadaFechaAndEstado(
                cuenta.mesa.id!!, cuenta.jornadaFecha, "DESPACHADO"
            )
            cuenta.toResponse(pedidos)
        }
    }

    private fun DiscoPedido.toResponse() = DiscoPedidoResponse(
        id = id!!,
        mesaId = mesa.id!!,
        mesaNumero = mesa.numero,
        mesaNombre = mesa.nombre,
        meseroId = mesero.id!!,
        meseroNombre = mesero.nombre,
        meseroColor = mesero.color,
        meseroAvatar = mesero.avatar,
        ticketDia = ticketDia,
        estado = estado,
        total = total,
        jornadaFecha = jornadaFecha,
        nota = nota,
        lineas = lineas.map { it.toResponse() },
        creadoEn = creadoEn.toString(),
        despachadoEn = despachadoEn?.toString()
    )

    private fun DiscoLineaPedido.toResponse() = DiscoLineaPedidoResponse(
        id = id!!,
        productoId = producto.id!!,
        nombre = nombre,
        precioUnitario = precioUnitario,
        cantidad = cantidad,
        total = total
    )

    private fun DiscoCuentaMesa.toResponse(pedidos: List<DiscoPedido>) = DiscoCuentaMesaResponse(
        id = id!!,
        mesaId = mesa.id!!,
        mesaNumero = mesa.numero,
        mesaNombre = mesa.nombre,
        nombreCliente = nombreCliente,
        meseroId = mesero.id!!,
        meseroNombre = mesero.nombre,
        meseroColor = mesero.color,
        meseroAvatar = mesero.avatar,
        jornadaFecha = jornadaFecha,
        total = total,
        estado = estado,
        pedidos = pedidos.map { it.toResponse() },
        creadoEn = creadoEn.toString()
    )

    private fun DiscoMesa.toResponse() = DiscoMesaResponse(
        id = id!!,
        numero = numero,
        nombre = nombre,
        estado = estado,
        nombreCliente = nombreCliente,
        meseroId = mesero?.id,
        meseroNombre = mesero?.nombre,
        meseroColor = mesero?.color,
        meseroAvatar = mesero?.avatar
    )
}
