package com.kompralo.controller

import com.kompralo.dto.CreateOrderRequest
import com.kompralo.dto.UpdateOrderRequest
import com.kompralo.dto.UpdateOrderStatusRequest
import com.kompralo.model.OrderStatus
import com.kompralo.repository.UserRepository
import com.kompralo.services.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class OrderController(
    private val orderService: OrderService,
    private val userRepository: UserRepository
) {

    private fun getSellerId(authentication: Authentication): Long {
        val email = authentication.name
        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        return user.id!!
    }

    @GetMapping
    fun getOrders(
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) search: String?,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val orders = orderService.getOrdersBySeller(email, status, search)
            ResponseEntity.ok(mapOf("orders" to orders, "total" to orders.size))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener pedidos")))
        }
    }

    @GetMapping("/stats")
    fun getOrderStats(authentication: Authentication): ResponseEntity<*> {
        return try {
            val email = authentication.name
            ResponseEntity.ok(orderService.getOrderStats(email))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener estadísticas")))
        }
    }

    @GetMapping("/export")
    fun exportOrders(
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val start = startDate?.let { LocalDateTime.parse(it) }
            val end = endDate?.let { LocalDateTime.parse(it) }
            val orders = orderService.getOrdersForExport(email, status, start, end)
            ResponseEntity.ok(orders)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al exportar pedidos")))
        }
    }

    @GetMapping("/{id}")
    fun getOrder(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(orderService.getOrderById(id, sellerId))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Pedido no encontrado")))
        }
    }

    @PostMapping
    fun createOrder(
        @RequestBody request: CreateOrderRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(sellerId, request))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al crear pedido")))
        }
    }

    @PutMapping("/{id}")
    fun updateOrder(
        @PathVariable id: Long,
        @RequestBody request: UpdateOrderRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(orderService.updateOrder(id, sellerId, request))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar pedido")))
        }
    }

    @PatchMapping("/{id}/status")
    fun updateOrderStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateOrderStatusRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(orderService.updateOrderStatus(id, sellerId, request.status))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar estado")))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteOrder(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            orderService.deleteOrder(id, sellerId)
            ResponseEntity.noContent().build<Void>()
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al eliminar pedido")))
        }
    }
}
