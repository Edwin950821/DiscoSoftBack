package com.kompralo.controller

import com.kompralo.dto.CreateSupplierRequest
import com.kompralo.dto.UpdateSupplierRequest
import com.kompralo.services.SupplierService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/suppliers")
class SupplierController(
    private val supplierService: SupplierService
) {

    @GetMapping
    fun getSuppliers(
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
        auth: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.getSuppliers(auth.name, includeInactive))
    }

    @GetMapping("/summaries")
    fun getSupplierSummaries(auth: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.getSupplierSummaries(auth.name))
    }

    @GetMapping("/{id}")
    fun getSupplier(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.getSupplier(auth.name, id))
    }

    @PostMapping
    fun createSupplier(
        @RequestBody request: CreateSupplierRequest,
        auth: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.createSupplier(auth.name, request))
    }

    @PutMapping("/{id}")
    fun updateSupplier(
        @PathVariable id: Long,
        @RequestBody request: UpdateSupplierRequest,
        auth: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.updateSupplier(auth.name, id, request))
    }

    @PatchMapping("/{id}/deactivate")
    fun deactivateSupplier(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.deactivateSupplier(auth.name, id))
    }

    @PatchMapping("/{id}/activate")
    fun activateSupplier(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.activateSupplier(auth.name, id))
    }

    @GetMapping("/stats")
    fun getStats(auth: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.getSupplierStats(auth.name))
    }

    @GetMapping("/metrics")
    fun getMetrics(auth: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.getSupplierMetrics(auth.name))
    }

    @GetMapping("/{id}/history")
    fun getPurchaseHistory(@PathVariable id: Long, auth: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(supplierService.getSupplierPurchaseHistory(auth.name, id))
    }
}
