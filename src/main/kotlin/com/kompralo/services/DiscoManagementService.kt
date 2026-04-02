package com.kompralo.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private val objectMapper = jacksonObjectMapper()

@Service
class DiscoManagementService(
    private val productoRepo: DiscoProductoRepository,
    private val meseroRepo: DiscoMeseroRepository,
    private val jornadaRepo: DiscoJornadaRepository,
    private val inventarioRepo: DiscoInventarioRepository,
    private val mesaRepo: DiscoMesaRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun getAllProductos(): List<DiscoProductoResponse> =
        productoRepo.findAllByOrderByCreadoEnDesc().map { it.toResponse() }

    @Transactional
    fun createProducto(req: DiscoProductoRequest): DiscoProductoResponse {
        val producto = DiscoProducto(
            nombre = req.nombre,
            precio = req.precio,
            activo = req.activo
        )
        return productoRepo.save(producto).toResponse()
    }

    @Transactional
    fun updateProducto(id: UUID, req: DiscoProductoUpdateRequest): DiscoProductoResponse {
        val producto = productoRepo.findById(id)
            .orElseThrow { RuntimeException("Producto no encontrado con id: $id") }

        val updated = producto.copy(
            nombre = req.nombre ?: producto.nombre,
            precio = req.precio ?: producto.precio,
            activo = req.activo ?: producto.activo
        )
        return productoRepo.save(updated).toResponse()
    }

    @Transactional
    fun deleteProducto(id: UUID) {
        val producto = productoRepo.findById(id)
            .orElseThrow { RuntimeException("Producto no encontrado con id: $id") }
        productoRepo.delete(producto)
    }

    fun getAllMeseros(): List<DiscoMeseroResponse> =
        meseroRepo.findAllByOrderByCreadoEnDesc().map { it.toResponse() }

    @Transactional
    fun createMesero(req: DiscoMeseroRequest): DiscoMeseroResponse {
        if (!req.username.isNullOrBlank() && !req.password.isNullOrBlank()) {
            val uname = req.username.trim().lowercase()
            val email = "$uname@monastery.co"

            if (userRepository.existsByUsername(uname)) {
                throw IllegalArgumentException("El username $uname ya esta registrado")
            }
            if (userRepository.existsByEmail(email)) {
                throw IllegalArgumentException("El email $email ya esta registrado")
            }

            val user = User(
                name = req.nombre,
                email = email,
                password = passwordEncoder.encode(req.password),
                role = Role.MESERO,
                isActive = true,
                username = uname
            )
            userRepository.save(user)
        }

        val uname = if (!req.username.isNullOrBlank()) req.username.trim().lowercase() else null
        val mesero = DiscoMesero(
            nombre = req.nombre,
            color = req.color,
            avatar = req.avatar,
            activo = req.activo,
            username = uname
        )
        return meseroRepo.save(mesero).toResponse()
    }

    @Transactional
    fun deleteMesero(id: UUID) {
        val mesero = meseroRepo.findById(id)
            .orElseThrow { RuntimeException("Mesero no encontrado con id: $id") }
        meseroRepo.delete(mesero)
    }

    fun getAllJornadas(): List<DiscoJornadaResponse> =
        jornadaRepo.findAllByOrderByCreadoEnDesc().map { it.toResponse() }

    @Transactional
    fun createJornada(req: DiscoJornadaRequest): DiscoJornadaResponse {
        val totalVendido = req.meseros.sumOf { it.totalMesero }
        val cortesias = req.meseros.sumOf { it.cortesias }
        val gastos = req.meseros.sumOf { it.gastos }

        val pagosEfectivo = req.meseros.sumOf { it.pagos["Efectivo"] ?: 0 }
        val pagosQR = req.meseros.sumOf { it.pagos["QR"] ?: 0 }
        val pagosNequi = req.meseros.sumOf { it.pagos["Nequi"] ?: 0 }
        val pagosDatafono = req.meseros.sumOf { it.pagos["Datafono"] ?: 0 }
        val pagosVales = req.meseros.sumOf { it.pagos["Vales"] ?: 0 }

        val totalRecibido = pagosEfectivo + pagosQR + pagosNequi + pagosDatafono + pagosVales
        val esperado = totalVendido - cortesias - gastos
        val saldo = totalRecibido - esperado

        val jornada = DiscoJornada(
            sesion = req.sesion,
            fecha = req.fecha,
            totalVendido = totalVendido,
            totalRecibido = totalRecibido,
            saldo = saldo,
            cortesias = cortesias,
            gastos = gastos,
            pagosEfectivo = pagosEfectivo,
            pagosQR = pagosQR,
            pagosNequi = pagosNequi,
            pagosDatafono = pagosDatafono,
            pagosVales = pagosVales
        )

        req.meseros.forEach { mReq ->
            val meseroJornada = DiscoMeseroJornada(
                meseroId = mReq.meseroId,
                nombre = mReq.nombre,
                color = mReq.color,
                avatar = mReq.avatar,
                totalMesero = mReq.totalMesero,
                cortesias = mReq.cortesias,
                gastos = mReq.gastos,
                pagosEfectivo = mReq.pagos["Efectivo"] ?: 0,
                pagosQR = mReq.pagos["QR"] ?: 0,
                pagosNequi = mReq.pagos["Nequi"] ?: 0,
                pagosDatafono = mReq.pagos["Datafono"] ?: 0,
                pagosVales = mReq.pagos["Vales"] ?: 0,
                transaccionesDetalle = if (mReq.transaccionesDetalle.isNotEmpty()) objectMapper.writeValueAsString(mReq.transaccionesDetalle) else null,
                valesDetalle = if (mReq.valesDetalle.isNotEmpty()) objectMapper.writeValueAsString(mReq.valesDetalle) else null,
                cortesiasDetalle = if (mReq.cortesiasDetalle.isNotEmpty()) objectMapper.writeValueAsString(mReq.cortesiasDetalle) else null,
                gastosDetalle = if (mReq.gastosDetalle.isNotEmpty()) objectMapper.writeValueAsString(mReq.gastosDetalle) else null,
                lineasDetalle = if (mReq.lineas.isNotEmpty()) objectMapper.writeValueAsString(mReq.lineas) else null
            )
            meseroJornada.jornada = jornada
            jornada.meseros.add(meseroJornada)
        }

        return jornadaRepo.save(jornada).toResponse()
    }

    @Transactional
    fun deleteJornada(id: UUID) {
        val jornada = jornadaRepo.findById(id)
            .orElseThrow { RuntimeException("Jornada no encontrada con id: $id") }
        jornadaRepo.delete(jornada)
    }

    fun getAllInventarios(): List<DiscoInventarioResponse> =
        inventarioRepo.findAllByOrderByCreadoEnDesc().map { it.toResponse() }

    @Transactional
    fun createInventario(req: DiscoInventarioRequest): DiscoInventarioResponse {
        val inventario = DiscoInventario(
            fecha = req.fecha,
            totalGeneral = req.totalGeneral
        )

        req.lineas.forEach { lReq ->
            val linea = DiscoLineaInventario(
                productoId = lReq.productoId,
                nombre = lReq.nombre,
                valorUnitario = lReq.valorUnitario,
                invInicial = lReq.invInicial,
                entradas = lReq.entradas,
                invFisico = lReq.invFisico,
                saldo = lReq.saldo,
                total = lReq.total
            )
            linea.inventario = inventario
            inventario.lineas.add(linea)
        }

        return inventarioRepo.save(inventario).toResponse()
    }

    @Transactional
    fun deleteInventario(id: UUID) {
        val inventario = inventarioRepo.findById(id)
            .orElseThrow { RuntimeException("Inventario no encontrado con id: $id") }
        inventarioRepo.delete(inventario)
    }

    fun getAllMesas(): List<DiscoMesaResponse> =
        mesaRepo.findAllByOrderByNumeroAsc().map { it.toResponse() }

    @Transactional
    fun createMesa(req: DiscoMesaRequest): DiscoMesaResponse {
        val mesa = DiscoMesa(
            numero = req.numero,
            nombre = req.nombre
        )
        return mesaRepo.save(mesa).toResponse()
    }

    @Transactional
    fun deleteMesa(id: UUID) {
        val mesa = mesaRepo.findById(id)
            .orElseThrow { RuntimeException("Mesa no encontrada con id: $id") }
        mesaRepo.delete(mesa)
    }

    private fun DiscoProducto.toResponse() = DiscoProductoResponse(
        id = id!!,
        nombre = nombre,
        precio = precio,
        activo = activo
    )

    private fun DiscoMesero.toResponse() = DiscoMeseroResponse(
        id = id!!,
        nombre = nombre,
        color = color,
        avatar = avatar,
        activo = activo,
        username = username
    )

    private fun DiscoJornada.toResponse() = DiscoJornadaResponse(
        id = id!!,
        sesion = sesion,
        fecha = fecha,
        meseros = meseros.map { it.toResponse() },
        pagos = mapOf(
            "Efectivo" to pagosEfectivo,
            "QR" to pagosQR,
            "Nequi" to pagosNequi,
            "Datafono" to pagosDatafono,
            "Vales" to pagosVales
        ),
        cortesias = cortesias,
        gastos = gastos,
        totalVendido = totalVendido,
        totalRecibido = totalRecibido,
        saldo = saldo
    )

    private fun DiscoMeseroJornada.toResponse() = DiscoMeseroJornadaResponse(
        meseroId = meseroId,
        nombre = nombre,
        color = color,
        avatar = avatar,
        totalMesero = totalMesero,
        cortesias = cortesias,
        gastos = gastos,
        pagos = mapOf(
            "Efectivo" to pagosEfectivo,
            "QR" to pagosQR,
            "Nequi" to pagosNequi,
            "Datafono" to pagosDatafono,
            "Vales" to pagosVales
        ),
        transaccionesDetalle = transaccionesDetalle?.let { objectMapper.readValue<List<TransaccionDetalleDTO>>(it) } ?: emptyList(),
        valesDetalle = valesDetalle?.let { objectMapper.readValue<List<ValeDetalleDTO>>(it) } ?: emptyList(),
        cortesiasDetalle = cortesiasDetalle?.let { objectMapper.readValue<List<CortesiaDetalleDTO>>(it) } ?: emptyList(),
        gastosDetalle = gastosDetalle?.let { objectMapper.readValue<List<GastoDetalleDTO>>(it) } ?: emptyList(),
        lineas = lineasDetalle?.let { objectMapper.readValue<List<LineaDetalleDTO>>(it) } ?: emptyList()
    )

    private fun DiscoInventario.toResponse() = DiscoInventarioResponse(
        id = id!!,
        fecha = fecha,
        lineas = lineas.map { it.toResponse() },
        totalGeneral = totalGeneral
    )

    private fun DiscoLineaInventario.toResponse() = DiscoLineaInventarioResponse(
        productoId = productoId,
        nombre = nombre,
        valorUnitario = valorUnitario,
        invInicial = invInicial,
        entradas = entradas,
        invFisico = invFisico,
        saldo = saldo,
        total = total
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
