package com.kompralo.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kompralo.config.TenantContext
import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
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
    private val comparativoRepo: DiscoComparativoRepository,
    private val promocionRepo: DiscoPromocionRepository,
    private val pedidoRepo: DiscoPedidoRepository,
    private val cuentaRepo: DiscoCuentaMesaRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tenantContext: TenantContext
) {

    private val tenantId: String get() = tenantContext.getNegocioId().toString()

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    fun getAllProductos(): List<DiscoProductoResponse> {
        val negocioId = tenantContext.getNegocioId()
        return productoRepo.findByNegocioIdOrderByCreadoEnDesc(negocioId).map { it.toResponse() }
    }

    @Transactional
    fun createProducto(req: DiscoProductoRequest): DiscoProductoResponse {
        val negocioId = tenantContext.getNegocioId()
        val producto = DiscoProducto(
            nombre = req.nombre,
            precio = req.precio,
            activo = req.activo,
            negocioId = negocioId
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

    fun getAllMeseros(): List<DiscoMeseroResponse> {
        val negocioId = tenantContext.getNegocioId()
        return meseroRepo.findByNegocioIdOrderByCreadoEnDesc(negocioId).map { it.toResponse() }
    }

    @Transactional
    fun createMesero(req: DiscoMeseroRequest): DiscoMeseroResponse {
        val negocioId = tenantContext.getNegocioId()

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
                username = uname,
                negocioId = negocioId
            )
            userRepository.save(user)
        }

        val uname = if (!req.username.isNullOrBlank()) req.username.trim().lowercase() else null
        val mesero = DiscoMesero(
            nombre = req.nombre,
            color = req.color,
            avatar = req.avatar,
            activo = req.activo,
            username = uname,
            negocioId = negocioId
        )
        return meseroRepo.save(mesero).toResponse()
    }

    @Transactional
    fun updateMesero(id: UUID, req: DiscoMeseroUpdateRequest): DiscoMeseroResponse {
        val mesero = meseroRepo.findById(id).orElseThrow { IllegalArgumentException("Mesero no encontrado") }
        req.nombre?.let { mesero.nombre = it }
        req.color?.let { mesero.color = it }
        req.avatar?.let { mesero.avatar = it }
        req.activo?.let { mesero.activo = it }
        val saved = meseroRepo.save(mesero)
        return DiscoMeseroResponse(saved.id!!, saved.nombre, saved.color, saved.avatar, saved.activo, saved.username)
    }

    @Transactional
    fun deleteMesero(id: UUID) {
        val negocioId = tenantContext.getNegocioId()

        // Obtener username via SQL nativo — NO cargamos entidad JPA
        @Suppress("UNCHECKED_CAST")
        val result = entityManager.createNativeQuery(
            "SELECT username FROM disco_meseros WHERE id = ?1 AND negocio_id = ?2"
        ).setParameter(1, id).setParameter(2, negocioId).resultList as List<String?>

        if (result.isEmpty()) {
            throw RuntimeException("Mesero no encontrado con id: $id")
        }
        val username = result[0]

        // SQL nativo en orden estricto — sin interferencia de Hibernate
        entityManager.createNativeQuery(
            "DELETE FROM disco_linea_pedido WHERE pedido_id IN (SELECT id FROM disco_pedidos WHERE mesero_id = ?1 AND negocio_id = ?2)"
        ).setParameter(1, id).setParameter(2, negocioId).executeUpdate()

        entityManager.createNativeQuery(
            "DELETE FROM disco_pedidos WHERE mesero_id = ?1 AND negocio_id = ?2"
        ).setParameter(1, id).setParameter(2, negocioId).executeUpdate()

        entityManager.createNativeQuery(
            "DELETE FROM disco_cuenta_mesa WHERE mesero_id = ?1 AND negocio_id = ?2"
        ).setParameter(1, id).setParameter(2, negocioId).executeUpdate()

        entityManager.createNativeQuery(
            "UPDATE disco_mesas SET mesero_id = NULL WHERE mesero_id = ?1 AND negocio_id = ?2"
        ).setParameter(1, id).setParameter(2, negocioId).executeUpdate()

        entityManager.createNativeQuery(
            "DELETE FROM disco_meseros WHERE id = ?1 AND negocio_id = ?2"
        ).setParameter(1, id).setParameter(2, negocioId).executeUpdate()

        // Eliminar usuario auth si existe
        if (!username.isNullOrBlank()) {
            entityManager.createNativeQuery(
                "DELETE FROM auth_users WHERE username = ?1"
            ).setParameter(1, username).executeUpdate()
        }
    }

    fun getAllJornadas(): List<DiscoJornadaResponse> {
        val negocioId = tenantContext.getNegocioId()
        return jornadaRepo.findByNegocioIdOrderByCreadoEnDesc(negocioId).map { it.toResponse() }
    }

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

        val negocioId = tenantContext.getNegocioId()
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
            pagosVales = pagosVales,
            negocioId = negocioId
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
                lineasDetalle = if (mReq.lineas.isNotEmpty()) objectMapper.writeValueAsString(mReq.lineas) else null,
                negocioId = negocioId
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

    fun getAllInventarios(): List<DiscoInventarioResponse> {
        val negocioId = tenantContext.getNegocioId()
        return inventarioRepo.findByNegocioIdOrderByCreadoEnDesc(negocioId).map { it.toResponse() }
    }

    @Transactional
    fun createInventario(req: DiscoInventarioRequest): DiscoInventarioResponse {
        val negocioId = tenantContext.getNegocioId()
        val inventario = DiscoInventario(
            fecha = req.fecha,
            totalGeneral = req.totalGeneral,
            negocioId = negocioId
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
                total = lReq.total,
                negocioId = negocioId
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

    fun getAllMesas(): List<DiscoMesaResponse> {
        val negocioId = tenantContext.getNegocioId()
        return mesaRepo.findByNegocioIdOrderByNumeroAsc(negocioId).map { it.toResponse() }
    }

    @Transactional
    fun createMesa(req: DiscoMesaRequest): DiscoMesaResponse {
        val negocioId = tenantContext.getNegocioId()
        val mesa = DiscoMesa(
            numero = req.numero,
            nombre = req.nombre,
            negocioId = negocioId
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

    fun getAllComparativos(): List<DiscoComparativoResponse> {
        val negocioId = tenantContext.getNegocioId()
        return comparativoRepo.findByNegocioIdOrderByCreadoEnDesc(negocioId).map { it.toResponse() }
    }

    @Transactional
    fun createComparativo(req: DiscoComparativoRequest): DiscoComparativoResponse {
        val negocioId = tenantContext.getNegocioId()
        val comparativo = DiscoComparativo(
            fecha = req.fecha,
            totalConteo = req.totalConteo,
            totalTiquets = req.totalTiquets,
            negocioId = negocioId
        )

        req.lineas.forEach { lReq ->
            val linea = DiscoLineaComparativo(
                productoId = lReq.productoId,
                nombre = lReq.nombre,
                conteo = lReq.conteo,
                tiquets = lReq.tiquets,
                diferencia = lReq.tiquets - lReq.conteo,
                negocioId = negocioId
            )
            linea.comparativo = comparativo
            comparativo.lineas.add(linea)
        }

        return comparativoRepo.save(comparativo).toResponse()
    }

    @Transactional
    fun deleteComparativo(id: UUID) {
        val comparativo = comparativoRepo.findById(id)
            .orElseThrow { RuntimeException("Comparativo no encontrado con id: $id") }
        comparativoRepo.delete(comparativo)
    }

    private fun DiscoComparativo.toResponse() = DiscoComparativoResponse(
        id = id!!,
        fecha = fecha,
        lineas = lineas.map { it.toResponse() },
        totalConteo = totalConteo,
        totalTiquets = totalTiquets
    )

    private fun DiscoLineaComparativo.toResponse() = DiscoLineaComparativoResponse(
        productoId = productoId,
        nombre = nombre,
        conteo = conteo,
        tiquets = tiquets,
        diferencia = diferencia
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

    fun getAllPromociones(): List<DiscoPromocionResponse> {
        val negocioId = tenantContext.getNegocioId()
        return promocionRepo.findByNegocioIdOrderByCreadoEnDesc(negocioId).map { it.toResponse() }
    }

    @Transactional
    fun createPromocion(req: DiscoPromocionRequest): DiscoPromocionResponse {
        val regaloProducto = productoRepo.findById(req.regaloProductoId)
            .orElseThrow { RuntimeException("Producto regalo no encontrado: ${req.regaloProductoId}") }

        req.compraProductoIds.forEach { id ->
            productoRepo.findById(id)
                .orElseThrow { RuntimeException("Producto de compra no encontrado: $id") }
        }

        val negocioId = tenantContext.getNegocioId()
        val promo = DiscoPromocion(
            nombre = req.nombre,
            compraProductoIds = req.compraProductoIds.joinToString(","),
            compraCantidad = req.compraCantidad,
            regaloProducto = regaloProducto,
            regaloCantidad = req.regaloCantidad,
            negocioId = negocioId
        )
        return promocionRepo.save(promo).toResponse()
    }

    @Transactional
    fun updatePromocion(id: UUID, req: DiscoPromocionUpdateRequest): DiscoPromocionResponse {
        val promo = promocionRepo.findById(id)
            .orElseThrow { RuntimeException("Promoción no encontrada con id: $id") }

        val regaloProducto = if (req.regaloProductoId != null) {
            productoRepo.findById(req.regaloProductoId)
                .orElseThrow { RuntimeException("Producto regalo no encontrado: ${req.regaloProductoId}") }
        } else promo.regaloProducto

        val compraIds = if (req.compraProductoIds != null) {
            req.compraProductoIds.joinToString(",")
        } else promo.compraProductoIds

        val updated = promo.copy(
            nombre = req.nombre ?: promo.nombre,
            compraProductoIds = compraIds,
            compraCantidad = req.compraCantidad ?: promo.compraCantidad,
            regaloProducto = regaloProducto,
            regaloCantidad = req.regaloCantidad ?: promo.regaloCantidad,
            activa = req.activa ?: promo.activa
        )
        return promocionRepo.save(updated).toResponse()
    }

    @Transactional
    fun deletePromocion(id: UUID) {
        val promo = promocionRepo.findById(id)
            .orElseThrow { RuntimeException("Promoción no encontrada con id: $id") }
        promocionRepo.delete(promo)
    }

    private fun DiscoPromocion.toResponse(): DiscoPromocionResponse {
        val ids = compraProductoIds.split(",").map { UUID.fromString(it.trim()) }
        val nombres = ids.mapNotNull { pid ->
            productoRepo.findById(pid).orElse(null)?.nombre
        }
        return DiscoPromocionResponse(
            id = id!!,
            nombre = nombre,
            compraProductoIds = ids,
            compraProductoNombres = nombres,
            compraCantidad = compraCantidad,
            regaloProductoId = regaloProducto.id!!,
            regaloProductoNombre = regaloProducto.nombre,
            regaloProductoPrecio = regaloProducto.precio,
            regaloCantidad = regaloCantidad,
            activa = activa
        )
    }
}
