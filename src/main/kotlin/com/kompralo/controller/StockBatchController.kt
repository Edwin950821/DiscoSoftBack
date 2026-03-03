package com.kompralo.controller

import com.kompralo.dto.BatchRestockRequest
import com.kompralo.repository.UserRepository
import com.kompralo.services.StockBatchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stock/batches")
class StockBatchController(
    private val stockBatchService: StockBatchService,
    private val userRepository: UserRepository
) {

    private fun getSellerId(authentication: Authentication): Long {
        val email = authentication.name
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        return user.id!!
    }

    @PostMapping
    fun createBatch(
        @RequestBody request: BatchRestockRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(stockBatchService.createBatch(sellerId, request))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al crear lote")))
        }
    }

    @GetMapping
    fun getBatches(
        @RequestParam(required = false) days: Int?,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(stockBatchService.getBatches(sellerId, days))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener lotes")))
        }
    }

    @GetMapping("/{id}")
    fun getBatch(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(stockBatchService.getBatchById(sellerId, id))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Lote no encontrado")))
        }
    }
}
