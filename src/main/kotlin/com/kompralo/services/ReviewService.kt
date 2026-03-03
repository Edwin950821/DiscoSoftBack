package com.kompralo.services

import com.kompralo.dto.CreateReviewRequest
import com.kompralo.dto.ReviewResponse
import com.kompralo.model.*
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.ReviewRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val notificationService: NotificationService,
) {

    @Transactional
    fun createReview(buyerEmail: String, request: CreateReviewRequest): ReviewResponse {
        if (request.rating < 1 || request.rating > 5) {
            throw RuntimeException("La calificacion debe ser entre 1 y 5")
        }

        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val product = productRepository.findById(request.productId)
            .orElseThrow { RuntimeException("Producto no encontrado") }

        if (reviewRepository.existsByProductIdAndBuyerId(request.productId, buyer.id!!)) {
            throw RuntimeException("Ya escribiste una resena para este producto")
        }

        val buyerOrders = orderRepository.findByBuyerOrderByCreatedAtDesc(buyer)
        val hasDeliveredOrder = buyerOrders.any { order ->
            order.status == OrderStatus.DELIVERED &&
                order.items.any { it.productId == request.productId }
        }
        if (!hasDeliveredOrder) {
            throw RuntimeException("Solo puedes resenar productos que hayas recibido")
        }

        val review = Review(
            product = product,
            buyer = buyer,
            rating = request.rating,
            comment = request.comment,
            imageUrls = request.imageUrls ?: emptyList(),
        )
        val saved = reviewRepository.save(review)

        recalculateRating(product)

        try {
            notificationService.createAndSend(
                userId = product.seller.id!!,
                type = NotificationType.NEW_REVIEW,
                title = "Nueva resena",
                message = "${buyer.name} califico '${product.name}' con ${request.rating} estrellas.",
                priority = "low",
                actionUrl = "/admin/products",
                relatedEntityId = product.id,
                relatedEntityType = RelatedEntityType.PRODUCT,
            )
        } catch (_: Exception) {}

        return saved.toResponse()
    }

    fun getReviewsByProduct(productId: Long): List<ReviewResponse> {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).map { it.toResponse() }
    }

    @Transactional
    fun deleteReview(reviewId: Long, buyerEmail: String) {
        val buyer = userRepository.findByEmail(buyerEmail)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val review = reviewRepository.findById(reviewId)
            .orElseThrow { RuntimeException("Resena no encontrada") }

        if (review.buyer.id != buyer.id) {
            throw RuntimeException("No autorizado para eliminar esta resena")
        }

        val product = review.product
        reviewRepository.delete(review)
        reviewRepository.flush()
        recalculateRating(product)
    }

    private fun recalculateRating(product: Product) {
        val avg = reviewRepository.averageRatingByProductId(product.id!!) ?: 0.0
        val count = reviewRepository.countByProductId(product.id!!).toInt()
        product.averageRating = avg
        product.reviewCount = count
        productRepository.save(product)
    }

    private fun Review.toResponse() = ReviewResponse(
        id = id!!,
        productId = product.id!!,
        buyerId = buyer.id!!,
        buyerName = buyer.name,
        rating = rating,
        comment = comment,
        imageUrls = imageUrls,
        createdAt = createdAt,
    )
}
