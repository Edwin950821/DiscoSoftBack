package com.kompralo.controller

import com.kompralo.dto.CreateOrderRequest
import com.kompralo.dto.UpdateOrderRequest
import com.kompralo.dto.UpdateOrderStatusRequest
import com.kompralo.model.OrderStatus
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.UserRepository
import com.kompralo.services.OrderService
import com.kompralo.services.PdfService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val pdfService: PdfService
) {
    private val logger = LoggerFactory.getLogger(OrderController::class.java)

    private fun getSellerId(authentication: Authentication): Long {
        val user = userRepository.findByEmail(authentication.name)
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
            val orders = orderService.getOrdersBySeller(authentication.name, status, search)
            ResponseEntity.ok(mapOf("orders" to orders, "total" to orders.size))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener pedidos")))
        }
    }

    @GetMapping("/stats")
    fun getOrderStats(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(orderService.getOrderStats(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener estadisticas")))
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
            val start = startDate?.let { LocalDateTime.parse(it) }
            val end = endDate?.let { LocalDateTime.parse(it) }
            val orders = orderService.getOrdersForExport(authentication.name, status, start, end)
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

    @PatchMapping("/{id}/confirm-payment")
    fun confirmPayment(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(orderService.confirmPayment(id, sellerId))
        } catch (e: Exception) {
            logger.error("confirmPayment error for order $id", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al confirmar pago")))
        }
    }

    @GetMapping("/{id}/receipt")
    fun downloadReceipt(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            val order = orderRepository.findByIdWithDetails(id)
                ?: throw RuntimeException("Pedido no encontrado")

            if (order.seller.id != sellerId) {
                throw RuntimeException("No autorizado para descargar este comprobante")
            }

            if (order.paymentStatus != "PAID") {
                throw RuntimeException("Solo se puede generar comprobante de pedidos con pago confirmado")
            }

            val pdfBytes = pdfService.generateReceiptById(id)
                ?: throw RuntimeException("Error al generar el comprobante PDF")

            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Factura_KMP-${order.orderNumber}.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.size.toLong())
                .body(pdfBytes)
        } catch (e: Exception) {
            logger.error("downloadReceipt error for order $id", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to (e.message ?: "Error al generar comprobante")))
        }
    }

    @PostMapping("/{id}/send-invoice")
    fun sendInvoice(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val sellerId = getSellerId(authentication)
            ResponseEntity.ok(orderService.sendInvoice(id, sellerId))
        } catch (e: Exception) {
            logger.error("sendInvoice error for order $id", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al enviar factura")))
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
