package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.services.DiscoManagementService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/disco/management")
@CrossOrigin(
    origins = ["http://localhost:5173", "http://localhost:3000"],
    allowCredentials = "true"
)
class DiscoManagementController(
    private val discoManagementService: DiscoManagementService
) {
    private val log = LoggerFactory.getLogger(DiscoManagementController::class.java)

    @GetMapping("/productos")
    fun getAllProductos(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(discoManagementService.getAllProductos())
        } catch (e: Exception) {
            log.error("Error al obtener productos: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener productos"))
        }
    }

    @PostMapping("/productos")
    fun createProducto(@RequestBody req: DiscoProductoRequest): ResponseEntity<*> {
        return try {
            val producto = discoManagementService.createProducto(req)
            ResponseEntity.status(HttpStatus.CREATED).body(producto)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Datos inválidos")))
        } catch (e: Exception) {
            log.error("Error al crear producto: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al crear producto"))
        }
    }

    @PatchMapping("/productos/{id}")
    fun updateProducto(
        @PathVariable id: UUID,
        @RequestBody req: DiscoProductoUpdateRequest
    ): ResponseEntity<*> {
        return try {
            val producto = discoManagementService.updateProducto(id, req)
            ResponseEntity.ok(producto)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Datos inválidos")))
        } catch (e: Exception) {
            log.error("Error al actualizar producto $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al actualizar producto"))
        }
    }

    @DeleteMapping("/productos/{id}")
    fun deleteProducto(@PathVariable id: UUID): ResponseEntity<Void> {
        return try {
            discoManagementService.deleteProducto(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            log.error("Error al eliminar producto $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/meseros")
    fun getAllMeseros(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(discoManagementService.getAllMeseros())
        } catch (e: Exception) {
            log.error("Error al obtener meseros: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener meseros"))
        }
    }

    @PostMapping("/meseros")
    fun createMesero(@RequestBody req: DiscoMeseroRequest): ResponseEntity<*> {
        return try {
            val mesero = discoManagementService.createMesero(req)
            ResponseEntity.status(HttpStatus.CREATED).body(mesero)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Datos inválidos")))
        } catch (e: Exception) {
            log.error("Error al crear mesero: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al crear mesero"))
        }
    }

    @DeleteMapping("/meseros/{id}")
    fun deleteMesero(@PathVariable id: UUID): ResponseEntity<Void> {
        return try {
            discoManagementService.deleteMesero(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            log.error("Error al eliminar mesero $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/jornadas")
    fun getAllJornadas(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(discoManagementService.getAllJornadas())
        } catch (e: Exception) {
            log.error("Error al obtener jornadas: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener jornadas"))
        }
    }

    @PostMapping("/jornadas")
    fun createJornada(@RequestBody req: DiscoJornadaRequest): ResponseEntity<*> {
        return try {
            val jornada = discoManagementService.createJornada(req)
            ResponseEntity.status(HttpStatus.CREATED).body(jornada)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Datos inválidos")))
        } catch (e: Exception) {
            log.error("Error al crear jornada: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al crear jornada"))
        }
    }

    @DeleteMapping("/jornadas/{id}")
    fun deleteJornada(@PathVariable id: UUID): ResponseEntity<Void> {
        return try {
            discoManagementService.deleteJornada(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            log.error("Error al eliminar jornada $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/inventarios")
    fun getAllInventarios(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(discoManagementService.getAllInventarios())
        } catch (e: Exception) {
            log.error("Error al obtener inventarios: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener inventarios"))
        }
    }

    @PostMapping("/inventarios")
    fun createInventario(@RequestBody req: DiscoInventarioRequest): ResponseEntity<*> {
        return try {
            val inventario = discoManagementService.createInventario(req)
            ResponseEntity.status(HttpStatus.CREATED).body(inventario)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Datos inválidos")))
        } catch (e: Exception) {
            log.error("Error al crear inventario: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al crear inventario"))
        }
    }

    @DeleteMapping("/inventarios/{id}")
    fun deleteInventario(@PathVariable id: UUID): ResponseEntity<Void> {
        return try {
            discoManagementService.deleteInventario(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            log.error("Error al eliminar inventario $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/mesas")
    fun getAllMesas(): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(discoManagementService.getAllMesas())
        } catch (e: Exception) {
            log.error("Error al obtener mesas: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al obtener mesas"))
        }
    }

    @PostMapping("/mesas")
    fun createMesa(@RequestBody req: DiscoMesaRequest): ResponseEntity<*> {
        return try {
            val mesa = discoManagementService.createMesa(req)
            ResponseEntity.status(HttpStatus.CREATED).body(mesa)
        } catch (e: Exception) {
            log.error("Error al crear mesa: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error al crear mesa"))
        }
    }

    @DeleteMapping("/mesas/{id}")
    fun deleteMesa(@PathVariable id: UUID): ResponseEntity<Void> {
        return try {
            discoManagementService.deleteMesa(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            log.error("Error al eliminar mesa $id: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
