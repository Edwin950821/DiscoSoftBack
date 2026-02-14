package com.kompralo.controller

import com.kompralo.dto.CreateProductRequest
import com.kompralo.dto.RestockRequest
import com.kompralo.dto.UpdateProductRequest
import com.kompralo.repository.UserRepository
import com.kompralo.services.ProductService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class ProductController(
    private val productService: ProductService,
    private val userRepository: UserRepository
) {

    private fun getSellerId(authentication: Authentication): Long {
        val email = authentication.name
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        return user.id!!
    }

    @GetMapping
    fun getProducts(
        @RequestParam(required = false) search: String?,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            val products = if (!search.isNullOrBlank()) {
                productService.searchProducts(sellerId, search)
            } else {
                productService.getProductsBySeller(sellerId)
            }
            ResponseEntity.ok(products)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener productos")))
        }
    }

    @GetMapping("/{id}")
    fun getProduct(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(productService.getProductById(id, sellerId))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Producto no encontrado")))
        }
    }

    @PostMapping
    fun createProduct(
        @RequestBody request: CreateProductRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(sellerId, request))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al crear producto")))
        }
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @RequestBody request: UpdateProductRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(productService.updateProduct(id, sellerId, request))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar producto")))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            productService.deleteProduct(id, sellerId)
            ResponseEntity.noContent().build<Void>()
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al eliminar producto")))
        }
    }

    @PostMapping("/restock")
    fun restockProduct(
        @RequestBody request: RestockRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(productService.restockProduct(sellerId, request))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al reabastecer stock")))
        }
    }

    @GetMapping("/{id}/restock-history")
    fun getRestockHistory(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(productService.getRestockHistory(id, sellerId))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al obtener historial")))
        }
    }
}
