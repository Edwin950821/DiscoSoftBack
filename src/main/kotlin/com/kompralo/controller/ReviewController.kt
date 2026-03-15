package com.kompralo.controller

import com.kompralo.dto.CreateReviewRequest
import com.kompralo.services.ReviewService
import jakarta.validation.Valid
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
        @Valid @RequestBody request: CreateReviewRequest,
    ): ResponseEntity<*> {
        val review = reviewService.createReview(authentication.name, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(review)
    }

    @GetMapping("/api/public/products/{productId}/reviews")
    fun getReviews(@PathVariable productId: Long): ResponseEntity<*> {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId))
    }

    @DeleteMapping("/api/reviews/{id}")
    fun deleteReview(
        authentication: Authentication,
        @PathVariable id: Long,
    ): ResponseEntity<*> {
        reviewService.deleteReview(id, authentication.name)
        return ResponseEntity.ok(mapOf("message" to "Resena eliminada"))
    }
}
