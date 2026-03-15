package com.kompralo.controller

import com.kompralo.dto.CreateOrderRequest
import com.kompralo.dto.UpdateOrderRequest
import com.kompralo.dto.UpdateOrderStatusRequest
import com.kompralo.exception.*
import com.kompralo.model.OrderStatus
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.UserRepository
import com.kompralo.services.OrderService
import com.kompralo.services.PdfService
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

    private fun getSellerId(authentication: Authentication): Long {
        val user = userRepository.findByEmail(authentication.name)
            .orElseThrow { EntityNotFoundException("Usuario", authentication.name) }
        return user.id!!
    }

    @GetMapping
    fun getOrders(
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) search: String?,
        authentication: Authentication
    ): ResponseEntity<*> {
        val orders = orderService.getOrdersBySeller(authentication.name, status, search)
        return ResponseEntity.ok(mapOf("orders" to orders, "total" to orders.size))
    }

    @GetMapping("/stats")
    fun getOrderStats(authentication: Authentication): ResponseEntity<*> {
        return ResponseEntity.ok(orderService.getOrderStats(authentication.name))
    }

    @GetMapping("/export")
    fun exportOrders(
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        authentication: Authentication
    ): ResponseEntity<*> {
        val start = startDate?.let { LocalDateTime.parse(it) }
        val end = endDate?.let { LocalDateTime.parse(it) }
        val orders = orderService.getOrdersForExport(authentication.name, status, start, end)
        return ResponseEntity.ok(orders)
    }

    @GetMapping("/{id}")
    fun getOrder(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(orderService.getOrderById(id, sellerId))
    }

    @PostMapping
    fun createOrder(
        @RequestBody request: CreateOrderRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orderService.createOrder(sellerId, request))
    }

    @PutMapping("/{id}")
    fun updateOrder(
        @PathVariable id: Long,
        @RequestBody request: UpdateOrderRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(orderService.updateOrder(id, sellerId, request))
    }

    @PatchMapping("/{id}/status")
    fun updateOrderStatus(
        @PathVariable id: Long,
        @RequestBody request: UpdateOrderStatusRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(orderService.updateOrderStatus(id, sellerId, request.status))
    }

    @PatchMapping("/{id}/confirm-payment")
    fun confirmPayment(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(orderService.confirmPayment(id, sellerId))
    }

    @GetMapping("/{id}/receipt")
    fun downloadReceipt(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        val order = orderRepository.findByIdWithDetails(id)
            ?: throw EntityNotFoundException("Pedido", id)

        if (order.seller.id != sellerId) {
            throw UnauthorizedActionException("No autorizado para descargar este comprobante")
        }

        if (order.paymentStatus != "PAID") {
            throw BusinessRuleViolationException("Solo se puede generar comprobante de pedidos con pago confirmado")
        }

        val pdfBytes = pdfService.generateReceiptById(id)
            ?: throw BusinessRuleViolationException("Error al generar el comprobante PDF")

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Factura_KMP-${order.orderNumber}.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdfBytes.size.toLong())
            .body(pdfBytes)
    }

    @PostMapping("/{id}/send-invoice")
    fun sendInvoice(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        return ResponseEntity.ok(orderService.sendInvoice(id, sellerId))
    }

    @DeleteMapping("/{id}")
    fun deleteOrder(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val sellerId = getSellerId(authentication)
        orderService.deleteOrder(id, sellerId)
        return ResponseEntity.noContent().build<Void>()
    }
}
