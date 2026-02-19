package com.kompralo.controller

import com.kompralo.dto.AdjustStockRequest
import com.kompralo.services.InventoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class InventoryController(
    private val inventoryService: InventoryService,
) {

    @GetMapping
    fun getInventoryItems(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(inventoryService.getInventoryItems(authentication.name, search, status))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener inventario")))
        }
    }

    @GetMapping("/{id}")
    fun getInventoryItem(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(inventoryService.getInventoryItem(authentication.name, id))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "No encontrado")))
        }
    }

    @PostMapping("/adjust")
    fun adjustStock(
        @RequestBody request: AdjustStockRequest,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(inventoryService.adjustStock(authentication.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al ajustar stock")))
        }
    }

    @GetMapping("/movements")
    fun getMovements(
        @RequestParam(required = false) productId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val safeSize = size.coerceIn(1, 100)
            ResponseEntity.ok(inventoryService.getMovements(authentication.name, productId, page, safeSize))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener movimientos")))
        }
    }
}
