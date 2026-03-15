package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.services.DiscoPedidoService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/disco/pedidos")
@CrossOrigin(
    origins = ["http://localhost:5173", "http://localhost:3000"],
    allowCredentials = "true"
)
class DiscoPedidoController(
    private val pedidoService: DiscoPedidoService
) {
    private val log = LoggerFactory.getLogger(DiscoPedidoController::class.java)

    @PostMapping("/mesas/{mesaId}/atender")
    fun atenderMesa(
        @PathVariable mesaId: UUID,
        @RequestBody req: DiscoAtenderMesaRequest
    ): ResponseEntity<*> {
        return try {
            val mesa = pedidoService.atenderMesa(mesaId, req)
            ResponseEntity.ok(mesa)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            log.error("Error al atender mesa: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al atender mesa"))
        }
    }

    @PostMapping
    fun crearPedido(@RequestBody req: DiscoPedidoRequest): ResponseEntity<*> {
        return try {
            val pedido = pedidoService.crearPedido(req)
            ResponseEntity.status(HttpStatus.CREATED).body(pedido)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            log.error("Error al crear pedido: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al crear pedido"))
        }
    }

    @PatchMapping("/{id}/despachar")
    fun despacharPedido(@PathVariable id: UUID): ResponseEntity<*> {
        return try {
            val pedido = pedidoService.despacharPedido(id)
            ResponseEntity.ok(pedido)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            log.error("Error al despachar pedido $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al despachar pedido"))
        }
    }

    @PatchMapping("/{id}/cancelar")
    fun cancelarPedido(@PathVariable id: UUID): ResponseEntity<*> {
        return try {
            val pedido = pedidoService.cancelarPedido(id)
            ResponseEntity.ok(pedido)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            log.error("Error al cancelar pedido $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al cancelar pedido"))
        }
    }

    @PutMapping("/{id}")
    fun editarPedido(
        @PathVariable id: UUID,
        @RequestBody req: DiscoPedidoRequest
    ): ResponseEntity<*> {
        return try {
            val pedido = pedidoService.editarPedido(id, req)
            ResponseEntity.ok(pedido)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            log.error("Error al editar pedido $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al editar pedido"))
        }
    }

    @PostMapping("/mesas/{mesaId}/pagar")
    fun pagarCuenta(@PathVariable mesaId: UUID): ResponseEntity<*> {
        return try {
            val cuenta = pedidoService.pagarCuenta(mesaId)
            ResponseEntity.ok(cuenta)
        } catch (e: Exception) {
            log.error("Error al pagar cuenta mesa $mesaId: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al pagar cuenta"))
        }
    }

    @GetMapping("/hoy")
    fun getPedidosHoy(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(pedidoService.getPedidosHoy())
        } catch (e: Exception) {
            log.error("Error al obtener pedidos: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener pedidos"))
        }
    }

    @GetMapping("/pendientes")
    fun getPedidosPendientes(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(pedidoService.getPedidosPendientes())
        } catch (e: Exception) {
            log.error("Error al obtener pedidos pendientes: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener pedidos pendientes"))
        }
    }

    @GetMapping("/mesas/{mesaId}/pedidos")
    fun getPedidosMesa(@PathVariable mesaId: UUID): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(pedidoService.getPedidosPorMesa(mesaId))
        } catch (e: Exception) {
            log.error("Error al obtener pedidos de mesa: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener pedidos"))
        }
    }

    @GetMapping("/mesas/{mesaId}/cuenta")
    fun getCuentaMesa(@PathVariable mesaId: UUID): ResponseEntity<*> {
        return try {
            val cuenta = pedidoService.getCuentaMesa(mesaId)
            if (cuenta != null) ResponseEntity.ok(cuenta)
            else ResponseEntity.ok(mapOf("message" to "No hay cuenta abierta para esta mesa"))
        } catch (e: Exception) {
            log.error("Error al obtener cuenta de mesa: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener cuenta"))
        }
    }

    @GetMapping("/cuentas/hoy")
    fun getCuentasHoy(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(pedidoService.getCuentasHoy())
        } catch (e: Exception) {
            log.error("Error al obtener cuentas: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener cuentas"))
        }
    }
}
