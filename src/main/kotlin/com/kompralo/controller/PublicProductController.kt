package com.kompralo.controller

import com.kompralo.dto.PublicProductResponse
import com.kompralo.model.ProductStatus
import com.kompralo.repository.ProductRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/products")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
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
                    productRepository.searchByStatusAndName(ProductStatus.ACTIVE, search)
                !category.isNullOrBlank() ->
                    productRepository.findByStatusAndCategoryContainingIgnoreCase(ProductStatus.ACTIVE, category)
                sellerId != null ->
                    productRepository.findBySellerIdAndStatus(sellerId, ProductStatus.ACTIVE)
                else ->
                    productRepository.findByStatus(ProductStatus.ACTIVE)
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
                    description = p.description,
                    sellerId = p.seller.id!!,
                    sellerName = p.seller.name,
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
            val product = productRepository.findById(id)
                .orElseThrow { RuntimeException("Producto no encontrado") }

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
                description = product.description,
                sellerId = product.seller.id!!,
                sellerName = product.seller.name,
            )

            ResponseEntity.ok(response)
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Producto no encontrado")))
        }
    }
}
