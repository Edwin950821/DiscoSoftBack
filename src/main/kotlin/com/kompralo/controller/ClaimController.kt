package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.model.ClaimStatus
import com.kompralo.services.ClaimService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class ClaimController(
    private val claimService: ClaimService
) {

    @PostMapping("/api/claims")
    fun createClaim(
        @RequestBody request: CreateClaimRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(claimService.createClaim(authentication.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/buyer/claims")
    fun getMyClaims(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.getMyClaims(authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/claims")
    fun getClaims(
        @RequestParam(required = false) status: ClaimStatus?,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.getClaims(authentication.name, status))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/claims/{id}")
    fun getClaim(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.getClaim(id))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/claims/stats")
    fun getClaimStats(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.getClaimStats(authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/claims/{id}/extend")
    fun extendClaim(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.extendClaim(id, authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/claims/{id}/refund")
    fun requestRefund(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.requestRefund(id, authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/claims/{id}/store-respond")
    fun storeRespond(
        @PathVariable id: Long,
        @RequestBody request: StoreRespondClaimRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.storeRespond(id, authentication.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/claims/{id}/admin-resolve")
    fun adminResolve(
        @PathVariable id: Long,
        @RequestBody request: AdminResolveClaimRequest
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(claimService.adminResolve(id, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }
}
