package com.kompralo.services

import com.kompralo.config.TenantContext
import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil

@Service
class DiscoBillarService(
    private val mesaBillarRepo: DiscoMesaBillarRepository,
    private val partidaRepo: DiscoPartidaBillarRepository,
    private val socketIO: SocketIOService,
    private val tenantContext: TenantContext
) {

    private val tenantId: String get() = tenantContext.getNegocioId().toString()

    // El "día" cambia a las 6AM Colombia, no a medianoche.
    // Así una jornada nocturna (ej: 2PM a 3AM) queda en una sola fecha.
    private val hoy: String get() {
        val ahora = LocalDateTime.now(ZoneId.of("America/Bogota"))
        val fechaJornada = if (ahora.hour < 6) ahora.minusDays(1) else ahora
        return fechaJornada.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    @Transactional(readOnly = true)
    fun getAllMesasBillar(): List<DiscoMesaBillarResponse> {
        val negocioId = tenantContext.getNegocioId()
        val mesas = mesaBillarRepo.findByNegocioIdAndActivoTrueOrderByNumeroAsc(negocioId)
        return mesas.map { mesa ->
            val partidaActiva = partidaRepo.findByMesaBillarIdAndEstado(mesa.id!!, "EN_JUEGO")
            mesa.toResponse(partidaActiva)
        }
    }

    @Transactional
    fun createMesaBillar(req: DiscoMesaBillarRequest): DiscoMesaBillarResponse {
        val negocioId = tenantContext.getNegocioId()
        val nextNumero = mesaBillarRepo.findMaxNumeroByNegocioId(negocioId) + 1

        val mesa = DiscoMesaBillar(
            numero = nextNumero,
            nombre = req.nombre,
            precioPorHora = req.precioPorHora,
            negocioId = negocioId
        )
        val saved = mesaBillarRepo.save(mesa)
        val response = saved.toResponse(null)
        socketIO.sendToAdmin("billar_mesa_creada", response, tenantId)
        return response
    }

    @Transactional
    fun updateMesaBillar(id: UUID, req: DiscoMesaBillarUpdateRequest): DiscoMesaBillarResponse {
        val mesa = mesaBillarRepo.findById(id)
            .orElseThrow { RuntimeException("Mesa de billar no encontrada") }

        req.nombre?.let { mesa.nombre = it }
        req.precioPorHora?.let { mesa.precioPorHora = it }
        req.activo?.let { mesa.activo = it }

        val saved = mesaBillarRepo.save(mesa)
        val partidaActiva = partidaRepo.findByMesaBillarIdAndEstado(id, "EN_JUEGO")
        return saved.toResponse(partidaActiva)
    }

    @Transactional
    fun deleteMesaBillar(id: UUID) {
        val mesa = mesaBillarRepo.findById(id)
            .orElseThrow { RuntimeException("Mesa de billar no encontrada") }
        if (mesa.estado == "EN_JUEGO") throw IllegalStateException("No se puede eliminar una mesa con partida activa")
        mesaBillarRepo.delete(mesa)
    }

    @Transactional
    fun iniciarPartida(mesaId: UUID, req: DiscoIniciarPartidaRequest): DiscoPartidaBillarResponse {
        val negocioId = tenantContext.getNegocioId()
        val mesa = mesaBillarRepo.findById(mesaId)
            .orElseThrow { RuntimeException("Mesa de billar no encontrada") }

        if (mesa.estado == "EN_JUEGO") throw IllegalStateException("Esta mesa ya tiene una partida activa")
        if (!mesa.activo) throw IllegalStateException("Esta mesa no esta activa")

        val precioHora = req.precioPorHora ?: mesa.precioPorHora

        val partida = DiscoPartidaBillar(
            mesaBillar = mesa,
            nombreCliente = req.nombreCliente,
            precioPorHora = precioHora,
            jornadaFecha = hoy,
            negocioId = negocioId
        )

        mesa.estado = "EN_JUEGO"
        mesaBillarRepo.save(mesa)
        val saved = partidaRepo.save(partida)

        val response = saved.toResponse()
        socketIO.sendToAdmin("billar_partida_iniciada", response, tenantId)
        return response
    }

    @Transactional
    fun finalizarPartida(mesaId: UUID): DiscoPartidaBillarResponse {
        val mesa = mesaBillarRepo.findById(mesaId)
            .orElseThrow { NoSuchElementException("Mesa de billar no encontrada") }

        val partida = partidaRepo.findByMesaBillarIdAndEstado(mesaId, "EN_JUEGO")
            ?: throw IllegalStateException("No hay partida activa en esta mesa")

        val ahora = LocalDateTime.now()
        val segundosJugados = ChronoUnit.SECONDS.between(partida.horaInicio, ahora)
        val horasCobradas = if (segundosJugados <= 0) 1 else ceil(segundosJugados.toDouble() / 3600.0).toInt()
        val total = horasCobradas * partida.precioPorHora

        partida.horaFin = ahora
        partida.horasCobradas = horasCobradas
        partida.total = total
        partida.estado = "FINALIZADA"

        mesa.estado = "LIBRE"
        mesaBillarRepo.save(mesa)
        val saved = partidaRepo.save(partida)

        val response = saved.toResponse()
        socketIO.sendToAdmin("billar_partida_finalizada", response, tenantId)
        return response
    }

    @Transactional
    fun trasladarPartida(mesaOrigenId: UUID, mesaDestinoId: UUID): DiscoPartidaBillarResponse {
        val mesaOrigen = mesaBillarRepo.findById(mesaOrigenId)
            .orElseThrow { NoSuchElementException("Mesa de origen no encontrada") }
        val mesaDestino = mesaBillarRepo.findById(mesaDestinoId)
            .orElseThrow { NoSuchElementException("Mesa de destino no encontrada") }

        val partida = partidaRepo.findByMesaBillarIdAndEstado(mesaOrigenId, "EN_JUEGO")
            ?: throw IllegalStateException("No hay partida activa en la mesa de origen")

        if (mesaDestino.estado == "EN_JUEGO") throw IllegalStateException("La mesa de destino ya tiene una partida activa")
        if (!mesaDestino.activo) throw IllegalStateException("La mesa de destino no esta activa")

        // Mover partida a mesa destino
        partida.mesaBillar = mesaDestino
        mesaOrigen.estado = "LIBRE"
        mesaDestino.estado = "EN_JUEGO"

        mesaBillarRepo.save(mesaOrigen)
        mesaBillarRepo.save(mesaDestino)
        val saved = partidaRepo.save(partida)

        val response = saved.toResponse()
        socketIO.sendToAdmin("billar_partida_trasladada", response, tenantId)
        return response
    }

    @Transactional(readOnly = true)
    fun getPartidasHoy(): List<DiscoPartidaBillarResponse> {
        val negocioId = tenantContext.getNegocioId()
        return partidaRepo.findByNegocioIdAndJornadaFechaOrderByCreadoEnDesc(negocioId, hoy).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getTotalBillarHoy(): Int {
        val negocioId = tenantContext.getNegocioId()
        val finalizadas = partidaRepo.findByNegocioIdAndJornadaFechaAndEstado(negocioId, hoy, "FINALIZADA")
        return finalizadas.sumOf { it.total ?: 0 }
    }

    private fun DiscoMesaBillar.toResponse(partidaActiva: DiscoPartidaBillar?) = DiscoMesaBillarResponse(
        id = id!!,
        numero = numero,
        nombre = nombre,
        precioPorHora = precioPorHora,
        estado = estado,
        activo = activo,
        partidaActiva = partidaActiva?.toResponse()
    )

    private fun DiscoPartidaBillar.toResponse() = DiscoPartidaBillarResponse(
        id = id!!,
        mesaBillarId = mesaBillar.id!!,
        mesaBillarNumero = mesaBillar.numero,
        mesaBillarNombre = mesaBillar.nombre,
        nombreCliente = nombreCliente,
        horaInicio = horaInicio.toString(),
        horaFin = horaFin?.toString(),
        precioPorHora = precioPorHora,
        horasCobradas = horasCobradas,
        total = total,
        estado = estado,
        jornadaFecha = jornadaFecha,
        creadoEn = creadoEn.toString()
    )
}
