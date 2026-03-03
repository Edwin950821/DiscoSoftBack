package com.kompralo.controller

import com.kompralo.dto.CheckoutRequest
import com.kompralo.dto.WompiConfirmRequest
import com.kompralo.dto.WompiInitRequest
import com.kompralo.services.CheckoutService
import com.kompralo.services.WompiInitService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/checkout")
class CheckoutController(
    private val checkoutService: CheckoutService,
    private val wompiInitService: WompiInitService,
) {

    @PostMapping
    fun checkout(
        @RequestBody request: CheckoutRequest,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val response = checkoutService.checkout(authentication.name, request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al procesar el pedido")))
        }
    }

    @PostMapping("/wompi/init")
    fun initWompiPayment(
        @RequestBody request: WompiInitRequest,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val response = wompiInitService.initializePayment(authentication.name, request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al inicializar pago")))
        }
    }

    @PostMapping("/wompi/confirm")
    fun confirmWompiCheckout(
        @RequestBody request: WompiConfirmRequest,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val response = checkoutService.checkoutWithWompi(authentication.name, request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al confirmar pago")))
        }
    }
}
