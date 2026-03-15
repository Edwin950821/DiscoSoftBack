package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.model.ClaimStatus
import com.kompralo.services.ClaimService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class ClaimController(
    private val claimService: ClaimService
) {

    @PostMapping("/api/claims")
    fun createClaim(
        @RequestBody request: CreateClaimRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.createClaim(authentication.name, request))
    }

    @GetMapping("/api/buyer/claims")
    fun getMyClaims(authentication: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.getMyClaims(authentication.name))
    }

    @GetMapping("/api/claims")
    fun getClaims(
        @RequestParam(required = false) status: ClaimStatus?,
        authentication: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.getClaims(authentication.name, status))
    }

    @GetMapping("/api/claims/{id}")
    fun getClaim(@PathVariable id: Long): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.getClaim(id))
    }

    @GetMapping("/api/claims/stats")
    fun getClaimStats(authentication: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.getClaimStats(authentication.name))
    }

    @PatchMapping("/api/claims/{id}/extend")
    fun extendClaim(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.extendClaim(id, authentication.name))
    }

    @PatchMapping("/api/claims/{id}/refund")
    fun requestRefund(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.requestRefund(id, authentication.name))
    }

    @PatchMapping("/api/claims/{id}/store-respond")
    fun storeRespond(
        @PathVariable id: Long,
        @RequestBody request: StoreRespondClaimRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.storeRespond(id, authentication.name, request))
    }

    @PatchMapping("/api/claims/{id}/admin-resolve")
    fun adminResolve(
        @PathVariable id: Long,
        @RequestBody request: AdminResolveClaimRequest
    ): ResponseEntity<*> {
        return ResponseEntity.ok(claimService.adminResolve(id, request))
    }
}
