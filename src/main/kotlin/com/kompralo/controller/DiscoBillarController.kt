package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.services.DiscoBillarService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/disco/billar")
class DiscoBillarController(
    private val billarService: DiscoBillarService
) {
    private val log = LoggerFactory.getLogger(DiscoBillarController::class.java)

    @GetMapping("/mesas")
    fun getAllMesas(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(billarService.getAllMesasBillar())
        } catch (e: Exception) {
            log.error("Error al obtener mesas de billar: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener mesas de billar"))
        }
    }

    @PostMapping("/mesas")
    fun createMesa(@RequestBody req: DiscoMesaBillarRequest): ResponseEntity<*> {
        return try {
            val mesa = billarService.createMesaBillar(req)
            ResponseEntity.status(HttpStatus.CREATED).body(mesa)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Datos invalidos")))
        } catch (e: Exception) {
            log.error("Error al crear mesa de billar: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al crear mesa de billar"))
        }
    }

    @PatchMapping("/mesas/{id}")
    fun updateMesa(@PathVariable id: UUID, @RequestBody req: DiscoMesaBillarUpdateRequest): ResponseEntity<*> {
        return try {
            val mesa = billarService.updateMesaBillar(id, req)
            ResponseEntity.ok(mesa)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Datos invalidos")))
        } catch (e: Exception) {
            log.error("Error al actualizar mesa de billar $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al actualizar mesa de billar"))
        }
    }

    @DeleteMapping("/mesas/{id}")
    fun deleteMesa(@PathVariable id: UUID): ResponseEntity<*> {
        return try {
            billarService.deleteMesaBillar(id)
            ResponseEntity.noContent().build<Void>()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to (e.message ?: "No se puede eliminar esta mesa")))
        } catch (e: Exception) {
            log.error("Error al eliminar mesa de billar $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al eliminar mesa de billar"))
        }
    }

    @PostMapping("/mesas/{mesaId}/iniciar")
    fun iniciarPartida(@PathVariable mesaId: UUID, @RequestBody req: DiscoIniciarPartidaRequest): ResponseEntity<*> {
        return try {
            val partida = billarService.iniciarPartida(mesaId, req)
            ResponseEntity.status(HttpStatus.CREATED).body(partida)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "No se puede iniciar partida")))
        } catch (e: Exception) {
            log.error("Error al iniciar partida en mesa $mesaId: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al iniciar partida"))
        }
    }

    @PostMapping("/mesas/{mesaId}/finalizar")
    fun finalizarPartida(@PathVariable mesaId: UUID): ResponseEntity<*> {
        return try {
            val partida = billarService.finalizarPartida(mesaId)
            ResponseEntity.ok(partida)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "No se puede finalizar")))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "No encontrado")))
        } catch (e: Exception) {
            log.error("Error al finalizar partida en mesa $mesaId: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al finalizar partida"))
        }
    }

    @GetMapping("/partidas/hoy")
    fun getPartidasHoy(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(billarService.getPartidasHoy())
        } catch (e: Exception) {
            log.error("Error al obtener partidas de hoy: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener partidas"))
        }
    }

    @GetMapping("/total-hoy")
    fun getTotalHoy(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(mapOf("total" to billarService.getTotalBillarHoy()))
        } catch (e: Exception) {
            log.error("Error al obtener total billar hoy: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener total"))
        }
    }
}
