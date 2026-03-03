package com.kompralo.controller

import com.kompralo.dto.CreateReviewRequest
import com.kompralo.services.ReviewService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class ReviewController(
    private val reviewService: ReviewService,
) {

    @PostMapping("/api/reviews")
    fun createReview(
        authentication: Authentication,
        @RequestBody request: CreateReviewRequest,
    ): ResponseEntity<*> {
        return try {
            val review = reviewService.createReview(authentication.name, request)
            ResponseEntity.status(HttpStatus.CREATED).body(review)
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error al crear resena")))
        }
    }

    @GetMapping("/api/public/products/{productId}/reviews")
    fun getReviews(@PathVariable productId: Long): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(reviewService.getReviewsByProduct(productId))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener resenas")))
        }
    }

    @DeleteMapping("/api/reviews/{id}")
    fun deleteReview(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<*> {
        return try {
            reviewService.deleteReview(id, authentication.name)
            ResponseEntity.ok(mapOf("message" to "Resena eliminada"))
        } catch (e: RuntimeException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Error al eliminar resena")))
        }
    }
}
