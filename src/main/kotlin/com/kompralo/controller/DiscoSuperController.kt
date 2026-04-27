package com.kompralo.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kompralo.dto.ConsolidadoResponse
import com.kompralo.dto.LineaDetalleDTO
import com.kompralo.dto.NegocioConsolidado
import com.kompralo.dto.TendenciaDia
import com.kompralo.dto.TopMesero
import com.kompralo.dto.TopProducto
import com.kompralo.repository.DiscoJornadaRepository
import com.kompralo.repository.NegocioRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val mapper = jacksonObjectMapper()
private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

@RestController
@RequestMapping("/api/disco/super")
@PreAuthorize("hasRole('SUPER')")
class DiscoSuperController(
    private val negocioRepository: NegocioRepository,
    private val jornadaRepository: DiscoJornadaRepository
) {
    private val log = LoggerFactory.getLogger(DiscoSuperController::class.java)

    @GetMapping("/consolidado")
    fun consolidado(): ResponseEntity<*> {
        return try {
            val negocios = negocioRepository.findByActivoTrue()
            val negociosIds = negocios.mapNotNull { it.id }.toSet()

            val jornadas = jornadaRepository.findAll()
                .filter { it.negocioId in negociosIds }

            val porNegocio = negocios.mapNotNull { neg ->
                val negId = neg.id ?: return@mapNotNull null
                val js = jornadas.filter { it.negocioId == negId }
                NegocioConsolidado(
                    negocioId = negId.toString(),
                    nombre = neg.nombre,
                    slug = neg.slug,
                    colorPrimario = neg.colorPrimario,
                    totalVendido = js.sumOf { it.totalVendido.toLong() },
                    totalRecibido = js.sumOf { it.totalRecibido.toLong() },
                    saldo = js.sumOf { it.saldo.toLong() },
                    jornadasCount = js.size
                )
            }.sortedByDescending { it.totalVendido }

            val pagosTotales = mapOf(
                "Efectivo" to jornadas.sumOf { it.pagosEfectivo.toLong() },
                "QR" to jornadas.sumOf { it.pagosQR.toLong() },
                "Nequi" to jornadas.sumOf { it.pagosNequi.toLong() },
                "Datafono" to jornadas.sumOf { it.pagosDatafono.toLong() },
                "Vales" to jornadas.sumOf { it.pagosVales.toLong() }
            )

            // === Comparativo mes actual vs mes anterior (basado en fecha YYYY-MM-DD) ===
            val hoy = LocalDate.now()
            val mesActual = hoy.month
            val anioActual = hoy.year
            val mesAnteriorDate = hoy.minusMonths(1)
            val mesAnterior = mesAnteriorDate.month
            val anioAnterior = mesAnteriorDate.year

            var totalMesActual = 0L
            var totalMesAnterior = 0L
            jornadas.forEach { j ->
                val fechaJornada = parseFechaSafe(j.fecha) ?: return@forEach
                if (fechaJornada.month == mesActual && fechaJornada.year == anioActual) {
                    totalMesActual += j.totalVendido.toLong()
                } else if (fechaJornada.month == mesAnterior && fechaJornada.year == anioAnterior) {
                    totalMesAnterior += j.totalVendido.toLong()
                }
            }

            // === Tendencia 30 dias (suma global por dia, ordenado asc) ===
            val hace30 = hoy.minusDays(29)
            val tendenciaMap = sortedMapOf<LocalDate, Long>()
            var cursor = hace30
            while (!cursor.isAfter(hoy)) {
                tendenciaMap[cursor] = 0L
                cursor = cursor.plusDays(1)
            }
            jornadas.forEach { j ->
                val fechaJornada = parseFechaSafe(j.fecha) ?: return@forEach
                if (!fechaJornada.isBefore(hace30) && !fechaJornada.isAfter(hoy)) {
                    tendenciaMap[fechaJornada] = (tendenciaMap[fechaJornada] ?: 0L) + j.totalVendido.toLong()
                }
            }
            val tendencia30Dias = tendenciaMap.map { (d, total) -> TendenciaDia(d.format(ISO_DATE), total) }

            // === Top 5 meseros (acumulado entre todos los negocios) ===
            data class MeseroAcc(var nombre: String, var color: String, var total: Long, var jornadas: MutableSet<java.util.UUID>)
            val meseroMap = mutableMapOf<java.util.UUID, MeseroAcc>()
            jornadas.forEach { j ->
                j.meseros.forEach { mj ->
                    val acc = meseroMap.getOrPut(mj.meseroId) {
                        MeseroAcc(mj.nombre, mj.color, 0L, mutableSetOf())
                    }
                    acc.total += mj.totalMesero.toLong()
                    j.id?.let { acc.jornadas.add(it) }
                }
            }
            val topMeseros = meseroMap.entries
                .map { (id, acc) -> TopMesero(id.toString(), acc.nombre, acc.color, acc.total, acc.jornadas.size) }
                .sortedByDescending { it.totalVendido }
                .take(5)

            // === Top 5 productos (de lineasDetalle JSON) ===
            data class ProdAcc(var nombre: String, var cantidad: Int, var total: Long)
            val prodMap = mutableMapOf<String, ProdAcc>()
            jornadas.forEach { j ->
                j.meseros.forEach meseroLoop@ { mj ->
                    val raw = mj.lineasDetalle ?: return@meseroLoop
                    if (raw.isBlank()) return@meseroLoop
                    val lineas: List<LineaDetalleDTO> = try {
                        mapper.readValue(raw)
                    } catch (e: Exception) {
                        return@meseroLoop
                    }
                    lineas.forEach lineaLoop@ { l ->
                        val key = l.productoId.ifBlank { l.nombre }
                        if (key.isBlank()) return@lineaLoop
                        val acc = prodMap.getOrPut(key) { ProdAcc(l.nombre, 0, 0L) }
                        acc.cantidad += l.cantidad
                        acc.total += l.total.toLong()
                    }
                }
            }
            val topProductos = prodMap.entries
                .map { (id, acc) -> TopProducto(id, acc.nombre, acc.cantidad, acc.total) }
                .sortedByDescending { it.total }
                .take(5)

            val response = ConsolidadoResponse(
                totalVendido = porNegocio.sumOf { it.totalVendido },
                totalRecibido = porNegocio.sumOf { it.totalRecibido },
                totalSaldo = porNegocio.sumOf { it.saldo },
                totalCortesias = jornadas.sumOf { it.cortesias.toLong() },
                totalGastos = jornadas.sumOf { it.gastos.toLong() },
                jornadasCount = jornadas.size,
                negociosCount = porNegocio.size,
                porNegocio = porNegocio,
                pagosTotales = pagosTotales,
                totalMesActual = totalMesActual,
                totalMesAnterior = totalMesAnterior,
                tendencia30Dias = tendencia30Dias,
                topProductos = topProductos,
                topMeseros = topMeseros
            )

            ResponseEntity.ok(response)

        } catch (e: Exception) {
            log.error("Error en consolidado: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener consolidado"))
        }
    }

    private fun parseFechaSafe(fecha: String): LocalDate? = try {
        LocalDate.parse(fecha, ISO_DATE)
    } catch (_: Exception) {
        null
    }
}
