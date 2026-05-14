package com.kompralo.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kompralo.dto.BarraComparativo
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
import com.kompralo.model.TipoNegocio
import org.springframework.web.bind.annotation.*
import java.time.DayOfWeek
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
    fun consolidado(
        @RequestParam(required = false) tipo: String?,
        @RequestParam(required = false) negocioId: String?,
        @RequestParam(required = false) rango: String?,
        @RequestParam(required = false) fechaDesde: String?,
        @RequestParam(required = false) fechaHasta: String?
    ): ResponseEntity<*> {
        if (!tipo.isNullOrBlank()) {
            val valid = TipoNegocio.values().any { it.name.equals(tipo, ignoreCase = true) }
            if (!valid) {
                return ResponseEntity.badRequest().body(mapOf(
                    "message" to "tipo invalido. Valores aceptados: DISCOTECA, BILLAR"
                ))
            }
        }

        val negocioUuid: java.util.UUID? = negocioId?.takeIf { it.isNotBlank() }?.let { raw ->
            try { java.util.UUID.fromString(raw.trim()) } catch (_: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf(
                    "message" to "negocioId invalido (debe ser UUID)"
                ))
            }
        }

        val desdeRaw = fechaDesde?.trim()?.takeIf { it.isNotBlank() }
        val hastaRaw = fechaHasta?.trim()?.takeIf { it.isNotBlank() }
        val tienePersonalizado = desdeRaw != null || hastaRaw != null
        if (tienePersonalizado && (desdeRaw == null || hastaRaw == null)) {
            return ResponseEntity.badRequest().body(mapOf(
                "message" to "fechaDesde y fechaHasta deben venir juntas"
            ))
        }
        val desdeCustom: LocalDate? = desdeRaw?.let { parseFechaSafe(it) }
        val hastaCustom: LocalDate? = hastaRaw?.let { parseFechaSafe(it) }
        if (tienePersonalizado && (desdeCustom == null || hastaCustom == null)) {
            return ResponseEntity.badRequest().body(mapOf(
                "message" to "fechas invalidas. Formato esperado: yyyy-MM-dd"
            ))
        }
        if (desdeCustom != null && hastaCustom != null && desdeCustom.isAfter(hastaCustom)) {
            return ResponseEntity.badRequest().body(mapOf(
                "message" to "fechaDesde no puede ser posterior a fechaHasta"
            ))
        }

        val rangosValidos = setOf("HOY", "7D", "30D", "MES_ACTUAL", "ESTE_ANO", "TODO", "PERSONALIZADO")
        val rangoFiltro: String = when {
            tienePersonalizado -> "PERSONALIZADO"
            else -> rango?.uppercase()?.takeIf { it.isNotBlank() } ?: "TODO"
        }
        if (rangoFiltro !in rangosValidos) {
            return ResponseEntity.badRequest().body(mapOf(
                "message" to "rango invalido. Valores aceptados: HOY, 7D, 30D, MES_ACTUAL, ESTE_ANO, TODO, PERSONALIZADO"
            ))
        }
        if (rangoFiltro == "PERSONALIZADO" && (desdeCustom == null || hastaCustom == null)) {
            return ResponseEntity.badRequest().body(mapOf(
                "message" to "rango PERSONALIZADO requiere fechaDesde y fechaHasta"
            ))
        }

        return try {
            val tipoFiltro: TipoNegocio? = tipo?.takeIf { it.isNotBlank() }?.uppercase()?.let { raw ->
                runCatching { TipoNegocio.valueOf(raw) }.getOrNull()
            }

            val negocios = negocioRepository.findByActivoTrue()
                .let { all -> if (negocioUuid != null) all.filter { it.id == negocioUuid }
                              else if (tipoFiltro != null) all.filter { it.tipo == tipoFiltro }
                              else all }
            val negociosIds = negocios.mapNotNull { it.id }.toSet()

            val hoy = LocalDate.now()
            val hace6 = hoy.minusDays(6)

            val fechaDesdeFiltro: LocalDate? = when (rangoFiltro) {
                "HOY" -> hoy
                "7D" -> hoy.minusDays(6)
                "30D" -> hoy.minusDays(29)
                "MES_ACTUAL" -> hoy.withDayOfMonth(1)
                "ESTE_ANO" -> hoy.withDayOfYear(1)
                "PERSONALIZADO" -> desdeCustom
                else -> null
            }
            val fechaHastaFiltro: LocalDate = if (rangoFiltro == "PERSONALIZADO") hastaCustom!! else hoy

            val jornadasGlobales = jornadaRepository.findAll()
                .filter { it.negocioId in negociosIds }

            val jornadas = if (fechaDesdeFiltro == null) jornadasGlobales
                else jornadasGlobales.filter { j ->
                    val f = parseFechaSafe(j.fecha)
                    f != null && !f.isBefore(fechaDesdeFiltro) && !f.isAfter(fechaHastaFiltro)
                }

            val porNegocio = negocios.mapNotNull { neg ->
                val negId = neg.id ?: return@mapNotNull null
                val js = jornadas.filter { it.negocioId == negId }
                val jsGlobales = jornadasGlobales.filter { it.negocioId == negId }

                val porDiaSemana = mutableMapOf<DayOfWeek, Long>()
                val sparkMap = sortedMapOf<LocalDate, Long>()
                var cur = hace6
                while (!cur.isAfter(hoy)) { sparkMap[cur] = 0L; cur = cur.plusDays(1) }

                js.forEach { j ->
                    val fecha = parseFechaSafe(j.fecha) ?: return@forEach
                    val v = j.totalVendido.toLong()
                    porDiaSemana.merge(fecha.dayOfWeek, v) { a, b -> a + b }
                }
                jsGlobales.forEach sparkLoop@ { j ->
                    val fecha = parseFechaSafe(j.fecha) ?: return@sparkLoop
                    if (!fecha.isBefore(hace6) && !fecha.isAfter(hoy)) {
                        sparkMap[fecha] = (sparkMap[fecha] ?: 0L) + j.totalVendido.toLong()
                    }
                }

                val diaTopEntry = porDiaSemana.maxByOrNull { it.value }
                val diaTop = if (diaTopEntry != null && diaTopEntry.value > 0) {
                    traducirDia(diaTopEntry.key)
                } else null

                NegocioConsolidado(
                    negocioId = negId.toString(),
                    nombre = neg.nombre,
                    slug = neg.slug,
                    colorPrimario = neg.colorPrimario,
                    tipo = neg.tipo.name,
                    totalVendido = js.sumOf { it.totalVendido.toLong() },
                    totalRecibido = js.sumOf { it.totalRecibido.toLong() },
                    saldo = js.sumOf { it.saldo.toLong() },
                    jornadasCount = js.size,
                    diaSemanaMasFuerte = diaTop,
                    sparkline7Dias = sparkMap.values.toList()
                )
            }.sortedByDescending { it.totalVendido }

            val pagosTotales = mapOf(
                "Efectivo" to jornadas.sumOf { it.pagosEfectivo.toLong() },
                "QR" to jornadas.sumOf { it.pagosQR.toLong() },
                "Nequi" to jornadas.sumOf { it.pagosNequi.toLong() },
                "Datafono" to jornadas.sumOf { it.pagosDatafono.toLong() },
                "Vales" to jornadas.sumOf { it.pagosVales.toLong() }
            )

            val mesActual = hoy.month
            val anioActual = hoy.year
            val mesAnteriorDate = hoy.minusMonths(1)
            val mesAnterior = mesAnteriorDate.month
            val anioAnterior = mesAnteriorDate.year

            var totalMesActual = 0L
            var totalMesAnterior = 0L
            jornadasGlobales.forEach { j ->
                val fechaJornada = parseFechaSafe(j.fecha) ?: return@forEach
                if (fechaJornada.month == mesActual && fechaJornada.year == anioActual) {
                    totalMesActual += j.totalVendido.toLong()
                } else if (fechaJornada.month == mesAnterior && fechaJornada.year == anioAnterior) {
                    totalMesAnterior += j.totalVendido.toLong()
                }
            }

            val hace30 = hoy.minusDays(29)
            val tendenciaMap = sortedMapOf<LocalDate, Long>()
            var cursor = hace30
            while (!cursor.isAfter(hoy)) {
                tendenciaMap[cursor] = 0L
                cursor = cursor.plusDays(1)
            }
            jornadasGlobales.forEach { j ->
                val fechaJornada = parseFechaSafe(j.fecha) ?: return@forEach
                if (!fechaJornada.isBefore(hace30) && !fechaJornada.isAfter(hoy)) {
                    tendenciaMap[fechaJornada] = (tendenciaMap[fechaJornada] ?: 0L) + j.totalVendido.toLong()
                }
            }
            val tendencia30Dias = tendenciaMap.map { (d, total) -> TendenciaDia(d.format(ISO_DATE), total) }

            data class MeseroAcc(
                var nombre: String,
                var color: String,
                var total: Long,
                var jornadas: MutableSet<java.util.UUID>,
                var negocioId: java.util.UUID
            )
            val negocioById = negocios.mapNotNull { it.id?.let { id -> id to it } }.toMap()
            val meseroMap = mutableMapOf<java.util.UUID, MeseroAcc>()
            jornadas.forEach { j ->
                j.meseros.forEach mjLoop@ { mj ->
                    if (mj.nombre.equals("barra", ignoreCase = true)) return@mjLoop
                    val acc = meseroMap.getOrPut(mj.meseroId) {
                        MeseroAcc(mj.nombre, mj.color, 0L, mutableSetOf(), mj.negocioId)
                    }
                    acc.total += mj.totalMesero.toLong()
                    j.id?.let { acc.jornadas.add(it) }
                }
            }
            val topMeseros = meseroMap.entries
                .map { (id, acc) ->
                    val neg = negocioById[acc.negocioId]
                    TopMesero(
                        meseroId = id.toString(),
                        nombre = acc.nombre,
                        color = acc.color,
                        totalVendido = acc.total,
                        jornadasCount = acc.jornadas.size,
                        negocioId = acc.negocioId.toString(),
                        negocioNombre = neg?.nombre ?: "Desconocido",
                        negocioColor = neg?.colorPrimario ?: "#888"
                    )
                }
                .sortedByDescending { it.totalVendido }
                .take(5)

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
                        val key = l.nombre.trim()
                        if (key.isBlank()) return@lineaLoop
                        val acc = prodMap.getOrPut(key) { ProdAcc(l.nombre, 0, 0L) }
                        acc.cantidad += l.cantidad
                        acc.total += l.total.toLong()
                    }
                }
            }
            val topProductos = prodMap.entries
                .map { (nombre, acc) -> TopProducto(nombre, acc.nombre, acc.cantidad, acc.total) }
                .sortedByDescending { it.total }
                .take(5)

            val comparativoBarras = negocios.mapNotNull { neg ->
                val negId = neg.id ?: return@mapNotNull null
                val js = jornadas.filter { it.negocioId == negId }
                val totalNegocio = js.sumOf { it.totalVendido.toLong() }

                var totalBarra = 0L
                var efectivoBarra = 0L
                var qrBarra = 0L
                var nequiBarra = 0L
                var datafonoBarra = 0L
                var valesBarra = 0L
                val jornadasBarra = mutableSetOf<java.util.UUID>()

                js.forEach { j ->
                    j.meseros.forEach { mj ->
                        if (mj.nombre.equals("barra", ignoreCase = true)) {
                            totalBarra += mj.totalMesero.toLong()
                            efectivoBarra += mj.pagosEfectivo.toLong()
                            qrBarra += mj.pagosQR.toLong()
                            nequiBarra += mj.pagosNequi.toLong()
                            datafonoBarra += mj.pagosDatafono.toLong()
                            valesBarra += mj.pagosVales.toLong()
                            j.id?.let { jornadasBarra.add(it) }
                        }
                    }
                }

                BarraComparativo(
                    negocioId = negId.toString(),
                    negocioNombre = neg.nombre,
                    negocioColor = neg.colorPrimario,
                    negocioTipo = neg.tipo.name,
                    totalVendido = totalBarra,
                    totalNegocio = totalNegocio,
                    pctDelNegocio = if (totalNegocio > 0) (totalBarra * 100.0) / totalNegocio else 0.0,
                    jornadasCount = jornadasBarra.size,
                    pagosEfectivo = efectivoBarra,
                    pagosQR = qrBarra,
                    pagosNequi = nequiBarra,
                    pagosDatafono = datafonoBarra,
                    pagosVales = valesBarra
                )
            }.sortedByDescending { it.totalVendido }

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
                topMeseros = topMeseros,
                comparativoBarras = comparativoBarras
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

    private fun traducirDia(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "Lunes"
        DayOfWeek.TUESDAY -> "Martes"
        DayOfWeek.WEDNESDAY -> "Miercoles"
        DayOfWeek.THURSDAY -> "Jueves"
        DayOfWeek.FRIDAY -> "Viernes"
        DayOfWeek.SATURDAY -> "Sabado"
        DayOfWeek.SUNDAY -> "Domingo"
    }
}
