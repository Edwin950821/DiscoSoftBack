package com.kompralo.controller

import com.kompralo.dto.CreateProductRequest
import com.kompralo.dto.RestockRequest
import com.kompralo.dto.UpdateProductRequest
import com.kompralo.repository.UserRepository
import com.kompralo.services.ProductService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasAnyRole('BUSINESS', 'OWNER', 'ADMIN')")
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
        val sellerId = getSellerId(authentication)
        val products = if (!search.isNullOrBlank()) {
            productService.searchProducts(sellerId, search)
        } else {
            productService.getProductsBySeller(sellerId)
        }
        return ResponseEntity.ok(products)
    }

    @GetMapping("/{id}")
    fun getProduct(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(productService.getProductById(id, sellerId))
    }

    @PostMapping
    fun createProduct(
        @Valid @RequestBody request: CreateProductRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(productService.createProduct(sellerId, request))
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(productService.updateProduct(id, sellerId, request))
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        productService.deleteProduct(id, sellerId)
        return ResponseEntity.noContent().build<Void>()
    }

    @PostMapping("/restock")
    fun restockProduct(
        @Valid @RequestBody request: RestockRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(productService.restockProduct(sellerId, request))
    }

    @GetMapping("/{id}/restock-history")
    fun getRestockHistory(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(productService.getRestockHistory(id, sellerId))
    }
}
