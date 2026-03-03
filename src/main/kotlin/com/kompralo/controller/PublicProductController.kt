package com.kompralo.controller

import com.kompralo.dto.PublicProductResponse
import com.kompralo.dto.PublicVariantDTO
import com.kompralo.model.ProductStatus
import com.kompralo.repository.ProductRepository
import org.springframework.http.HttpStatus
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
    ): ResponseEntity<*> {
        return try {
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

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener productos")))
        }
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            val product = productRepository.findByIdWithDetails(id)
                ?: throw RuntimeException("Producto no encontrado")

            if (product.status != ProductStatus.ACTIVE) {
                throw RuntimeException("Producto no disponible")
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

            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Producto no encontrado")))
        }
    }
}
