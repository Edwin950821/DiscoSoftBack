package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.model.ReturnStatus
import com.kompralo.services.ReturnService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class ReturnController(
    private val returnService: ReturnService
) {

    @PostMapping("/api/returns")
    fun createReturn(
        @RequestBody request: CreateReturnRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED).body(returnService.createReturn(authentication.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/buyer/returns")
    fun getMyReturns(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.getMyReturns(authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/returns")
    fun getReturns(
        @RequestParam(required = false) status: ReturnStatus?,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.getReturns(authentication.name, status))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/returns/{id}")
    fun getReturn(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.getReturn(id))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @GetMapping("/api/returns/stats")
    fun getReturnStats(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.getReturnStats(authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/returns/{id}/approve")
    fun approveReturn(
        @PathVariable id: Long,
        @RequestBody request: ApproveReturnRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.approveReturn(id, authentication.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/returns/{id}/reject")
    fun rejectReturn(
        @PathVariable id: Long,
        @RequestBody request: RejectReturnRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.rejectReturn(id, authentication.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/returns/{id}/escalate")
    fun escalateReturn(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.escalateReturn(id, authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/returns/{id}/mark-refunded")
    fun markRefundIssued(
        @PathVariable id: Long,
        @RequestBody request: MarkRefundIssuedRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.markRefundIssued(id, authentication.name, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/buyer/returns/{id}/confirm-refund")
    fun confirmRefundReceived(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.confirmRefundReceived(id, authentication.name))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }

    @PatchMapping("/api/returns/{id}/admin-resolve")
    fun adminResolveReturn(
        @PathVariable id: Long,
        @RequestBody request: AdminResolveReturnRequest
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(returnService.adminResolve(id, request))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error")))
        }
    }
}
