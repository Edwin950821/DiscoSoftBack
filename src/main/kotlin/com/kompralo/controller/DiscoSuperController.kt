package com.kompralo.controller

import com.kompralo.dto.ConsolidadoResponse
import com.kompralo.dto.NegocioConsolidado
import com.kompralo.repository.DiscoJornadaRepository
import com.kompralo.repository.NegocioRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

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

            val response = ConsolidadoResponse(
                totalVendido = porNegocio.sumOf { it.totalVendido },
                totalRecibido = porNegocio.sumOf { it.totalRecibido },
                totalSaldo = porNegocio.sumOf { it.saldo },
                totalCortesias = jornadas.sumOf { it.cortesias.toLong() },
                totalGastos = jornadas.sumOf { it.gastos.toLong() },
                jornadasCount = jornadas.size,
                negociosCount = porNegocio.size,
                porNegocio = porNegocio,
                pagosTotales = pagosTotales
            )

            ResponseEntity.ok(response)

        } catch (e: Exception) {
            log.error("Error en consolidado: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener consolidado"))
        }
    }
}
