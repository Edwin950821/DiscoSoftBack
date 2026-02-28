package com.kompralo.controller

import com.kompralo.dto.CreateSupplierRequest
import com.kompralo.dto.UpdateSupplierRequest
import com.kompralo.services.SupplierService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class SupplierController(
    private val supplierService: SupplierService
) {

    @GetMapping
    fun getSuppliers(
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
        auth: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.getSuppliers(auth.name, includeInactive))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @GetMapping("/summaries")
    fun getSupplierSummaries(auth: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.getSupplierSummaries(auth.name))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @GetMapping("/{id}")
    fun getSupplier(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.getSupplier(auth.name, id))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @PostMapping
    fun createSupplier(
        @RequestBody request: CreateSupplierRequest,
        auth: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.createSupplier(auth.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @PutMapping("/{id}")
    fun updateSupplier(
        @PathVariable id: Long,
        @RequestBody request: UpdateSupplierRequest,
        auth: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.updateSupplier(auth.name, id, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @PatchMapping("/{id}/deactivate")
    fun deactivateSupplier(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.deactivateSupplier(auth.name, id))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @PatchMapping("/{id}/activate")
    fun activateSupplier(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.activateSupplier(auth.name, id))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @GetMapping("/stats")
    fun getStats(auth: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.getSupplierStats(auth.name))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @GetMapping("/metrics")
    fun getMetrics(auth: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.getSupplierMetrics(auth.name))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @GetMapping("/{id}/history")
    fun getPurchaseHistory(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(supplierService.getSupplierPurchaseHistory(auth.name, id))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }
}
