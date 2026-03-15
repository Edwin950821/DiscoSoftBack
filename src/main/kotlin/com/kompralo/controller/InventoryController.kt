package com.kompralo.controller

import com.kompralo.dto.AdjustStockRequest
import com.kompralo.services.InventoryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/inventory")
class InventoryController(
    private val inventoryService: InventoryService,
) {

    @GetMapping
    fun getInventoryItems(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(inventoryService.getInventoryItems(authentication.name, search, status))
    }

    @GetMapping("/{id}")
    fun getInventoryItem(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(inventoryService.getInventoryItem(authentication.name, id))
    }

    @PostMapping("/adjust")
    fun adjustStock(
        @RequestBody request: AdjustStockRequest,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(inventoryService.adjustStock(authentication.name, request))
    }

    @GetMapping("/movements")
    fun getMovements(
        @RequestParam(required = false) productId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        authentication: Authentication,
    ): ResponseEntity<*> {
        val safeSize = size.coerceIn(1, 100)
        return ResponseEntity.ok(inventoryService.getMovements(authentication.name, productId, page, safeSize))
    }
}
