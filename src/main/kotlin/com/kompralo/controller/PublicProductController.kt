package com.kompralo.controller

import com.kompralo.dto.PublicProductResponse
import com.kompralo.dto.PublicVariantDTO
import com.kompralo.exception.*
import com.kompralo.model.ProductStatus
import com.kompralo.repository.ProductRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/products")
class PublicProductController(
    private val productRepository: ProductRepository,
) {

    @GetMapping
    fun getProducts(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) sellerId: Long?,
    ): ResponseEntity<List<PublicProductResponse>> {
        val products = when {
            !search.isNullOrBlank() ->
                productRepository.searchByStatusAndNameWithDetails(ProductStatus.ACTIVE, search)
            !category.isNullOrBlank() ->
                productRepository.findByStatusAndCategoryWithDetails(ProductStatus.ACTIVE, category)
            sellerId != null ->
                productRepository.findBySellerIdAndStatusWithDetails(sellerId, ProductStatus.ACTIVE)
            else ->
                productRepository.findByStatusWithDetails(ProductStatus.ACTIVE)
        }

        val response = products.map { p ->
            PublicProductResponse(
                id = p.id!!,
                name = p.name,
                sku = p.sku,
                category = p.category,
                price = p.price,
                stock = p.stock,
                imageUrl = p.imageUrl,
                imageUrls = p.images.sortedBy { it.position }.map { it.url },
                variants = p.variants.filter { it.active }.map { v ->
                    PublicVariantDTO(id = v.id!!, name = v.name, priceAdjustment = v.priceAdjustment, stock = v.stock, imageUrl = v.imageUrl)
                },
                description = p.description,
                sellerId = p.seller.id!!,
                sellerName = p.seller.name,
                averageRating = p.averageRating,
                reviewCount = p.reviewCount,
            )
        }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): ResponseEntity<PublicProductResponse> {
        val product = productRepository.findByIdWithDetails(id)
            ?: throw EntityNotFoundException("Producto", id)

        if (product.status != ProductStatus.ACTIVE) {
            throw BusinessRuleViolationException("Producto no disponible")
        }

        val response = PublicProductResponse(
            id = product.id!!,
            name = product.name,
            sku = product.sku,
            category = product.category,
            price = product.price,
            stock = product.stock,
            imageUrl = product.imageUrl,
            imageUrls = product.images.sortedBy { it.position }.map { it.url },
            variants = product.variants.filter { it.active }.map { v ->
                PublicVariantDTO(id = v.id!!, name = v.name, priceAdjustment = v.priceAdjustment, stock = v.stock, imageUrl = v.imageUrl)
            },
            description = product.description,
            sellerId = product.seller.id!!,
            sellerName = product.seller.name,
            averageRating = product.averageRating,
            reviewCount = product.reviewCount,
        )

        return ResponseEntity.ok(response)
    }
}
