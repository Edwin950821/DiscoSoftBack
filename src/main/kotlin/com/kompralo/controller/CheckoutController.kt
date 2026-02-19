package com.kompralo.controller

import com.kompralo.dto.CheckoutRequest
import com.kompralo.services.CheckoutService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class CheckoutController(
    private val checkoutService: CheckoutService,
) {

    @PostMapping
    fun checkout(
        @RequestBody request: CheckoutRequest,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            println("[Checkout] User: ${authentication.name}, Items: ${request.items.size}, PaymentMethod: ${request.paymentMethod}")
            val response = checkoutService.checkout(authentication.name, request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            println("[Checkout] ERROR: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al procesar el pedido")))
        }
    }
}
