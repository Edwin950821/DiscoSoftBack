package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class DiscoPedidoService(
    private val pedidoRepo: DiscoPedidoRepository,
    private val mesaRepo: DiscoMesaRepository,
    private val meseroRepo: DiscoMeseroRepository,
    private val productoRepo: DiscoProductoRepository,
    private val cuentaRepo: DiscoCuentaMesaRepository,
    private val promocionRepo: DiscoPromocionRepository,
    private val partidaBillarRepo: DiscoPartidaBillarRepository,
    private val jornadaDiariaRepo: DiscoJornadaDiariaRepository,
    private val socketIO: SocketIOService
) {

    // El "día" cambia a las 6AM Colombia, no a medianoche.
    // Así una jornada nocturna (ej: 2PM a 3AM) queda en una sola fecha.
    private val hoy: String get() {
        val ahora = LocalDateTime.now(ZoneId.of("America/Bogota"))
        val fechaJornada = if (ahora.hour < 6) ahora.minusDays(1) else ahora
        return fechaJornada.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

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
            cuentaRepo.saveAndFlush(cuenta)
        }

        val response = mesaRepo.saveAndFlush(mesa).toResponse()
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

        val cuenta = cuentaRepo.findByMesaIdAndEstado(mesa.id!!, "ABIERTA")
            ?: throw IllegalStateException("No hay cuenta abierta para esta mesa")

        val pedido = DiscoPedido(
            mesa = mesa,
            mesero = mesero,
            ticketDia = ticketDia,
            jornadaFecha = hoy,
            nota = req.nota,
            cuenta = cuenta
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
        val saved = pedidoRepo.saveAndFlush(pedido)
        val response = saved.toResponse()
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

        val cuenta = pedido.cuenta
            ?: cuentaRepo.findByMesaIdAndEstado(pedido.mesa.id!!, "ABIERTA")
            ?: throw RuntimeException("No hay cuenta abierta para esta mesa")

        if (!pedido.esCortesia) {
            cuenta.total += pedido.total
        }
        cuentaRepo.saveAndFlush(cuenta)

        val response = pedidoRepo.saveAndFlush(pedido).toResponse()
        pedido.mesero?.id?.let { socketIO.sendToMesero(it.toString(), "pedido_despachado", response) }
        socketIO.sendToAdmin("pedido_despachado", response)

        if (!pedido.esCortesia && pedido.mesero != null) {
            aplicarPromosAutomaticas(pedido.mesa, pedido.mesero!!, cuenta)
        }

        return response
    }

    private fun aplicarPromosAutomaticas(mesa: DiscoMesa, mesero: DiscoMesero, cuenta: DiscoCuentaMesa) {
        val promosActivas = promocionRepo.findByActivaTrue()
        if (promosActivas.isEmpty()) return

        val pedidosRegulares = pedidoRepo.findByCuentaIdAndEsCortesiaFalseAndEstado(
            cuenta.id!!, "DESPACHADO"
        )
        val cortesiasExistentes = pedidoRepo.findByCuentaIdAndEsCortesiaTrueAndEstadoNot(
            cuenta.id!!, "CANCELADO"
        )

        val productosAcumulados = mutableMapOf<UUID, Int>()
        pedidosRegulares.forEach { p ->
            p.lineas.forEach { l ->
                val pid = l.producto.id!!
                productosAcumulados[pid] = (productosAcumulados[pid] ?: 0) + l.cantidad
            }
        }

        var descuentoTotal = 0

        promosActivas.forEach { promo ->
            val compraIds = promo.compraProductoIds.split(",").map { UUID.fromString(it.trim()) }
            val regaloId = promo.regaloProducto.id!!
            val esMismoProducto = compraIds.size == 1 && compraIds[0] == regaloId

            val totalCompra = compraIds.sumOf { productosAcumulados[it] ?: 0 }
            if (totalCompra == 0) return@forEach

            val cortesiasYaDadas = cortesiasExistentes
                .filter { it.promo?.id == promo.id }
                .flatMap { it.lineas }
                .sumOf { it.cantidad }

            val cortesiasCalificadas: Int
            if (esMismoProducto) {
                val grupo = promo.compraCantidad + promo.regaloCantidad
                val sets = totalCompra / grupo
                cortesiasCalificadas = sets * promo.regaloCantidad
            } else {
                val sets = totalCompra / promo.compraCantidad
                cortesiasCalificadas = sets * promo.regaloCantidad
            }

            val nuevasCortesias = cortesiasCalificadas - cortesiasYaDadas
            if (nuevasCortesias <= 0) return@forEach

            val ticketDia = (pedidoRepo.countByJornadaFecha(cuenta.jornadaFecha) + 1).toInt()

            val cortesiaPedido = DiscoPedido(
                mesa = mesa,
                mesero = mesero,
                ticketDia = ticketDia,
                jornadaFecha = cuenta.jornadaFecha,
                estado = "DESPACHADO",
                despachadoEn = LocalDateTime.now(),
                esCortesia = true,
                promo = promo,
                nota = "Cortesia: ${promo.nombre}",
                cuenta = cuenta
            )

            val regaloProducto = promo.regaloProducto
            val lineaTotal = regaloProducto.precio * nuevasCortesias

            val linea = DiscoLineaPedido(
                producto = regaloProducto,
                nombre = regaloProducto.nombre,
                precioUnitario = regaloProducto.precio,
                cantidad = nuevasCortesias,
                total = lineaTotal
            )
            linea.pedido = cortesiaPedido
            cortesiaPedido.lineas.add(linea)
            cortesiaPedido.total = lineaTotal

            pedidoRepo.saveAndFlush(cortesiaPedido)
            descuentoTotal += lineaTotal

            val cortesiaResponse = cortesiaPedido.toResponse()
            socketIO.sendToAdmin("cortesia_aplicada", cortesiaResponse)
            socketIO.sendToMesero(mesero.id.toString(), "cortesia_aplicada", cortesiaResponse)
        }

        if (descuentoTotal > 0) {
            val totalCortesias = cortesiasExistentes.sumOf { it.total } + descuentoTotal
            cuenta.descuentoPromo = totalCortesias
            cuentaRepo.saveAndFlush(cuenta)
        }
    }

    @Transactional
    fun cancelarPedido(pedidoId: UUID): DiscoPedidoResponse {
        val pedido = pedidoRepo.findById(pedidoId)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (pedido.estado == "CANCELADO") {
            throw IllegalStateException("El pedido ya está cancelado")
        }

        if (pedido.estado == "DESPACHADO") {
            val cuenta = pedido.cuenta?.takeIf { it.estado == "ABIERTA" }
                ?: cuentaRepo.findByMesaIdAndEstado(pedido.mesa.id!!, "ABIERTA")
            if (cuenta != null) {
                if (!pedido.esCortesia) {
                    cuenta.total -= pedido.total
                    if (cuenta.total < 0) cuenta.total = 0
                }

                pedido.estado = "CANCELADO"
                pedido.canceladoEn = LocalDateTime.now()
                pedidoRepo.saveAndFlush(pedido)

                if (!pedido.esCortesia && pedido.mesero != null) {
                    recalcularCortesias(pedido.mesa, pedido.mesero!!, cuenta)
                } else {
                    val cortesiasVivas = pedidoRepo.findByCuentaIdAndEsCortesiaTrueAndEstadoNot(
                        cuenta.id!!, "CANCELADO"
                    )
                    cuenta.descuentoPromo = cortesiasVivas.sumOf { it.total }
                }
                cuentaRepo.saveAndFlush(cuenta)
            } else {
                pedido.estado = "CANCELADO"
                pedido.canceladoEn = LocalDateTime.now()
                pedidoRepo.saveAndFlush(pedido)
            }
        } else {
            pedido.estado = "CANCELADO"
            pedido.canceladoEn = LocalDateTime.now()
            pedidoRepo.saveAndFlush(pedido)
        }

        val response = pedido.toResponse()
        pedido.mesero?.id?.let { socketIO.sendToMesero(it.toString(), "pedido_cancelado", response) }
        socketIO.sendToAdmin("pedido_cancelado", response)
        return response
    }

    private fun recalcularCortesias(mesa: DiscoMesa, mesero: DiscoMesero, cuenta: DiscoCuentaMesa) {
        val promosActivas = promocionRepo.findByActivaTrue()
        if (promosActivas.isEmpty()) return

        val pedidosRegulares = pedidoRepo.findByCuentaIdAndEsCortesiaFalseAndEstado(
            cuenta.id!!, "DESPACHADO"
        )
        val cortesiasVivas = pedidoRepo.findByCuentaIdAndEsCortesiaTrueAndEstadoNot(
            cuenta.id!!, "CANCELADO"
        )

        val productosAcumulados = mutableMapOf<UUID, Int>()
        pedidosRegulares.forEach { p ->
            p.lineas.forEach { l ->
                val pid = l.producto.id!!
                productosAcumulados[pid] = (productosAcumulados[pid] ?: 0) + l.cantidad
            }
        }

        promosActivas.forEach { promo ->
            val compraIds = promo.compraProductoIds.split(",").map { UUID.fromString(it.trim()) }
            val regaloId = promo.regaloProducto.id!!
            val esMismoProducto = compraIds.size == 1 && compraIds[0] == regaloId

            val totalCompra = compraIds.sumOf { productosAcumulados[it] ?: 0 }

            val cortesiasCalificadas: Int = if (esMismoProducto) {
                val grupo = promo.compraCantidad + promo.regaloCantidad
                (totalCompra / grupo) * promo.regaloCantidad
            } else {
                (totalCompra / promo.compraCantidad) * promo.regaloCantidad
            }

            val cortesiasDeEstaPromo = cortesiasVivas.filter { it.promo?.id == promo.id }
            val cortesiasActuales = cortesiasDeEstaPromo.flatMap { it.lineas }.sumOf { it.cantidad }

            if (cortesiasActuales > cortesiasCalificadas) {
                val sobran = cortesiasActuales - cortesiasCalificadas
                var porCancelar = sobran
                for (c in cortesiasDeEstaPromo.reversed()) {
                    if (porCancelar <= 0) break
                    c.estado = "CANCELADO"
                    c.canceladoEn = LocalDateTime.now()
                    pedidoRepo.saveAndFlush(c)
                    porCancelar -= c.lineas.sumOf { it.cantidad }
                    socketIO.sendToAdmin("cortesia_cancelada", c.toResponse())
                }
            }
        }

        val cortesiasFinales = pedidoRepo.findByCuentaIdAndEsCortesiaTrueAndEstadoNot(
            cuenta.id!!, "CANCELADO"
        )
        cuenta.descuentoPromo = cortesiasFinales.sumOf { it.total }
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
            val cuenta = pedido.cuenta?.takeIf { it.estado == "ABIERTA" }
                ?: cuentaRepo.findByMesaIdAndEstado(pedido.mesa.id!!, "ABIERTA")
            if (cuenta != null) {
                cuenta.total = cuenta.total - totalAnterior + nuevoTotal
                cuentaRepo.saveAndFlush(cuenta)
            }
        }

        return pedidoRepo.saveAndFlush(pedido).toResponse()
    }

    @Transactional
    fun aplicarPromos(mesaId: UUID): DiscoCuentaMesaResponse {
        val cuenta = cuentaRepo.findByMesaIdAndEstado(mesaId, "ABIERTA")
            ?: throw RuntimeException("No hay cuenta abierta para esta mesa")

        cuenta.mesero?.let { aplicarPromosAutomaticas(cuenta.mesa, it, cuenta) }

        val pedidosRegulares = pedidoRepo.findByCuentaIdAndEsCortesiaFalseAndEstado(
            cuenta.id!!, "DESPACHADO"
        )
        val pedidosCortesia = pedidoRepo.findByCuentaIdAndEsCortesiaTrueAndEstadoNot(
            cuenta.id!!, "CANCELADO"
        )
        return cuenta.toResponse(pedidosRegulares + pedidosCortesia)
    }

    @Transactional
    fun pagarCuenta(mesaId: UUID): DiscoCuentaMesaResponse {
        val cuenta = cuentaRepo.findByMesaIdAndEstado(mesaId, "ABIERTA")
            ?: throw RuntimeException("No hay cuenta abierta para esta mesa")

        val pedidosRegulares = pedidoRepo.findByCuentaIdAndEsCortesiaFalseAndEstado(
            cuenta.id!!, "DESPACHADO"
        )
        val pedidosCortesia = pedidoRepo.findByCuentaIdAndEsCortesiaTrueAndEstadoNot(
            cuenta.id!!, "CANCELADO"
        )

        val totalFromPedidos = pedidosRegulares.sumOf { it.total }
        cuenta.total = totalFromPedidos
        cuenta.descuentoPromo = pedidosCortesia.sumOf { it.total }
        cuenta.estado = "PAGADA"
        cuenta.pagadaEn = LocalDateTime.now()
        cuentaRepo.saveAndFlush(cuenta)

        liberarMesa(mesaId)

        val todosPedidos = pedidosRegulares + pedidosCortesia
        val response = cuenta.toResponse(todosPedidos)
        cuenta.mesero?.id?.let { socketIO.sendToMesero(it.toString(), "cuenta_pagada", response) }
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
        val pedidosRegulares = pedidoRepo.findByCuentaIdAndEsCortesiaFalseAndEstado(
            cuenta.id!!, "DESPACHADO"
        )
        val pedidosCortesia = pedidoRepo.findByCuentaIdAndEsCortesiaTrueAndEstadoNot(
            cuenta.id!!, "CANCELADO"
        )
        return cuenta.toResponse(pedidosRegulares + pedidosCortesia)
    }

    @Transactional(readOnly = true)
    fun getResumenDia(): DiscoResumenDiaResponse {
        val fecha = hoy

        val cuentasHoy = cuentaRepo.findByJornadaFechaOrderByCreadoEnDesc(fecha)
        val cuentasPagadas = cuentasHoy.filter { it.estado == "PAGADA" }
        val cuentasAbiertas = cuentasHoy.filter { it.estado == "ABIERTA" }

        val totalVentas = cuentasPagadas.sumOf { it.total - it.descuentoPromo }

        val ticketsTotales = pedidoRepo.countByJornadaFecha(fecha).toInt()
        val mesasAtendidas = cuentasHoy.map { it.mesa.id }.distinct().size

        val partidasHoy = partidaBillarRepo.findByJornadaFechaOrderByCreadoEnDesc(fecha)
        val partidasFinalizadas = partidasHoy.filter { it.estado == "FINALIZADA" }
        val totalBillar = partidasFinalizadas.sumOf { it.total ?: 0 }

        val jornadaCerrada = jornadaDiariaRepo.findByFecha(fecha) != null

        return DiscoResumenDiaResponse(
            fecha = fecha,
            totalVentas = totalVentas,
            totalBillar = totalBillar,
            totalGeneral = totalVentas + totalBillar,
            cuentasCerradas = cuentasPagadas.size,
            cuentasAbiertas = cuentasAbiertas.size,
            ticketsTotales = ticketsTotales,
            mesasAtendidas = mesasAtendidas,
            partidasBillar = partidasFinalizadas.size,
            jornadaCerrada = jornadaCerrada
        )
    }

    @Transactional(readOnly = true)
    fun getHistorialJornadas(): List<DiscoResumenJornadaResponse> =
        jornadaDiariaRepo.findAllByOrderByCerradoEnDesc().map { it.toResponse() }

    @Transactional
    fun cerrarJornada(): DiscoResumenJornadaResponse {
        val fecha = hoy

        jornadaDiariaRepo.findByFecha(fecha)?.let {
            throw IllegalStateException("La jornada de hoy ya fue cerrada")
        }

        val cuentasAbiertas = cuentaRepo.findByJornadaFechaOrderByCreadoEnDesc(fecha)
            .filter { it.estado == "ABIERTA" }
        if (cuentasAbiertas.isNotEmpty()) {
            throw IllegalStateException("Hay ${cuentasAbiertas.size} cuenta(s) abierta(s). Ciérrelas antes de cerrar la jornada.")
        }

        val resumen = getResumenDia()

        val jornada = DiscoJornadaDiaria(
            fecha = fecha,
            totalVentas = resumen.totalVentas,
            totalBillar = resumen.totalBillar,
            totalGeneral = resumen.totalGeneral,
            cuentasCerradas = resumen.cuentasCerradas,
            ticketsTotales = resumen.ticketsTotales,
            mesasAtendidas = resumen.mesasAtendidas,
            partidasBillar = resumen.partidasBillar
        )

        val saved = jornadaDiariaRepo.saveAndFlush(jornada)
        val response = saved.toResponse()
        socketIO.broadcast("jornada_cerrada", response)
        return response
    }

    @Transactional(readOnly = true)
    fun getCuentasHoy(): List<DiscoCuentaMesaResponse> {
        val cuentas = cuentaRepo.findByJornadaFechaOrderByCreadoEnDesc(hoy)
        return cuentas.map { cuenta ->
            val pedidos = pedidoRepo.findByCuentaIdAndEstado(cuenta.id!!, "DESPACHADO")
            cuenta.toResponse(pedidos)
        }
    }

    private fun DiscoPedido.toResponse() = DiscoPedidoResponse(
        id = id!!,
        mesaId = mesa.id!!,
        mesaNumero = mesa.numero,
        mesaNombre = mesa.nombre,
        meseroId = mesero?.id ?: UUID(0, 0),
        meseroNombre = mesero?.nombre ?: "Eliminado",
        meseroColor = mesero?.color ?: "#999999",
        meseroAvatar = mesero?.avatar ?: "",
        ticketDia = ticketDia,
        estado = estado,
        total = total,
        jornadaFecha = jornadaFecha,
        nota = nota,
        esCortesia = esCortesia,
        promoNombre = promo?.nombre,
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

    private fun DiscoCuentaMesa.toResponse(
        pedidos: List<DiscoPedido>
    ): DiscoCuentaMesaResponse {
        val pedidosRegulares = pedidos.filter { !it.esCortesia }
        val pedidosCortesia = pedidos.filter { it.esCortesia && it.estado != "CANCELADO" }
        val totalReal = if (pedidosRegulares.isNotEmpty()) pedidosRegulares.sumOf { it.total } else total
        val descuento = if (pedidosCortesia.isNotEmpty()) pedidosCortesia.sumOf { it.total } else descuentoPromo
        return DiscoCuentaMesaResponse(
            id = id!!,
            mesaId = mesa.id!!,
            mesaNumero = mesa.numero,
            mesaNombre = mesa.nombre,
            nombreCliente = nombreCliente,
            meseroId = mesero?.id ?: UUID(0, 0),
            meseroNombre = mesero?.nombre ?: "Eliminado",
            meseroColor = mesero?.color ?: "#999999",
            meseroAvatar = mesero?.avatar ?: "",
            jornadaFecha = jornadaFecha,
            total = totalReal,
            descuentoPromo = descuento,
            totalConDescuento = if (descuento > 0) totalReal - descuento else null,
            estado = estado,
            pedidos = pedidos.map { it.toResponse() },
            creadoEn = creadoEn.toString()
        )
    }

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

    private fun DiscoJornadaDiaria.toResponse() = DiscoResumenJornadaResponse(
        id = id!!,
        fecha = fecha,
        totalVentas = totalVentas,
        totalBillar = totalBillar,
        totalGeneral = totalGeneral,
        cuentasCerradas = cuentasCerradas,
        ticketsTotales = ticketsTotales,
        mesasAtendidas = mesasAtendidas,
        partidasBillar = partidasBillar,
        cerradoEn = cerradoEn.toString()
    )
}
