package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ReturnRequestRepository
import com.kompralo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ReturnService(
    private val returnRepository: ReturnRequestRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(ReturnService::class.java)

    @Transactional
    fun createReturn(email: String, request: CreateReturnRequest): ReturnResponse {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val order = orderRepository.findById(request.orderId)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (order.buyer.id != buyer.id) {
            throw RuntimeException("No autorizado para solicitar devolucion de este pedido")
        }

        if (order.status != OrderStatus.DELIVERED) {
            throw RuntimeException("Solo se pueden devolver pedidos entregados")
        }

        if (order.deliveredAt == null) {
            throw RuntimeException("No se puede determinar la fecha de entrega")
        }

        val daysSinceDelivery = ChronoUnit.DAYS.between(order.deliveredAt, LocalDateTime.now())
        if (daysSinceDelivery > 7) {
            throw RuntimeException("El plazo de devolucion de 7 dias ha vencido")
        }

        if (returnRepository.existsByOrderId(request.orderId)) {
            throw RuntimeException("Ya existe una solicitud de devolucion para este pedido")
        }

        val returnReq = ReturnRequest(
            order = order,
            buyer = buyer,
            seller = order.seller,
            reason = request.reason,
            description = request.description,
            requestedSolution = request.requestedSolution,
            refundAmount = order.total
        )
        request.imageUrls.forEach { returnReq.imageUrls.add(it) }

        val saved = returnRepository.save(returnReq)

        notificationService.createAndSend(
            userId = order.seller.id!!,
            type = NotificationType.NEW_ORDER,
            title = "Nueva solicitud de devolucion",
            message = "El comprador ${buyer.name} solicito devolucion del pedido ${order.orderNumber}.",
            priority = "high",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    fun getMyReturns(email: String): List<ReturnResponse> {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        return returnRepository.findByBuyerOrderByCreatedAtDesc(buyer).map { it.toResponse() }
    }

    fun getReturns(email: String, status: ReturnStatus?): List<ReturnResponse> {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val returns = if (status != null) {
            returnRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, status)
        } else {
            returnRepository.findBySellerOrderByCreatedAtDesc(seller)
        }
        return returns.map { it.toResponse() }
    }

    fun getReturn(id: Long): ReturnResponse {
        val ret = returnRepository.findById(id)
            .orElseThrow { RuntimeException("Devolucion no encontrada") }
        return ret.toResponse()
    }

    fun getReturnStats(email: String): ReturnStatsResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        return ReturnStatsResponse(
            total = returnRepository.countBySeller(seller),
            pending = returnRepository.countBySellerAndStatus(seller, ReturnStatus.PENDING),
            inReview = returnRepository.countBySellerAndStatus(seller, ReturnStatus.IN_REVIEW),
            approved = returnRepository.countBySellerAndStatus(seller, ReturnStatus.APPROVED),
            rejected = returnRepository.countBySellerAndStatus(seller, ReturnStatus.REJECTED),
            escalated = returnRepository.countBySellerAndStatus(seller, ReturnStatus.ESCALATED),
            refundIssued = returnRepository.countBySellerAndStatus(seller, ReturnStatus.REFUND_ISSUED),
            completed = returnRepository.countBySellerAndStatus(seller, ReturnStatus.COMPLETED)
        )
    }

    @Transactional
    fun approveReturn(id: Long, email: String, request: ApproveReturnRequest): ReturnResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val ret = returnRepository.findById(id)
            .orElseThrow { RuntimeException("Devolucion no encontrada") }

        if (ret.seller.id != seller.id) {
            throw RuntimeException("No autorizado")
        }

        if (ret.status != ReturnStatus.PENDING && ret.status != ReturnStatus.IN_REVIEW) {
            throw RuntimeException("Esta devolucion no puede ser aprobada en estado ${ret.status}")
        }

        ret.status = ReturnStatus.APPROVED
        ret.requiresProductReturn = request.requiresProductReturn
        ret.refundAmount = request.refundAmount
        ret.resolvedAt = LocalDateTime.now()

        val saved = returnRepository.save(ret)

        // Notificar al comprador
        notificationService.createAndSend(
            userId = ret.buyer.id!!,
            type = NotificationType.ORDER_CONFIRMED,
            title = "Devolucion aprobada",
            message = "Tu devolucion del pedido ${ret.order.orderNumber} fue aprobada. Monto: $${request.refundAmount}.",
            priority = "high",
            actionUrl = "/mis-pedidos",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        // Notificar a la tienda que debe reembolsar
        notificationService.createAndSend(
            userId = seller.id!!,
            type = NotificationType.PAYMENT_SUCCESS,
            title = "Debes realizar un reembolso",
            message = "Debes reembolsar $${request.refundAmount} al comprador ${ret.buyer.name} por el pedido ${ret.order.orderNumber}.",
            priority = "high",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun rejectReturn(id: Long, email: String, request: RejectReturnRequest): ReturnResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val ret = returnRepository.findById(id)
            .orElseThrow { RuntimeException("Devolucion no encontrada") }

        if (ret.seller.id != seller.id) {
            throw RuntimeException("No autorizado")
        }

        ret.status = ReturnStatus.REJECTED
        ret.storeResponse = request.reason
        ret.resolvedAt = LocalDateTime.now()

        val saved = returnRepository.save(ret)

        notificationService.createAndSend(
            userId = ret.buyer.id!!,
            type = NotificationType.ORDER_CANCELLED,
            title = "Devolucion rechazada",
            message = "Tu devolucion del pedido ${ret.order.orderNumber} fue rechazada. Motivo: ${request.reason}.",
            priority = "high",
            actionUrl = "/mis-pedidos",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun adminResolve(id: Long, request: AdminResolveReturnRequest): ReturnResponse {
        val ret = returnRepository.findById(id)
            .orElseThrow { RuntimeException("Devolucion no encontrada") }

        ret.status = request.status
        ret.adminNotes = request.notes
        if (request.status == ReturnStatus.COMPLETED) {
            ret.resolvedAt = LocalDateTime.now()

            if (ret.refundAmount > java.math.BigDecimal.ZERO) {
                val order = ret.order
                order.status = OrderStatus.REFUNDED
                order.paymentStatus = "REFUNDED"
                orderRepository.save(order)
            }
        }

        val saved = returnRepository.save(ret)

        notificationService.createAndSend(
            userId = ret.buyer.id!!,
            type = NotificationType.PAYMENT_SUCCESS,
            title = "Devolucion completada",
            message = "La devolucion del pedido ${ret.order.orderNumber} fue completada. Reembolso: $${ret.refundAmount}.",
            priority = "high",
            actionUrl = "/mis-pedidos",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun escalateReturn(id: Long, email: String): ReturnResponse {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val ret = returnRepository.findById(id)
            .orElseThrow { RuntimeException("Devolucion no encontrada") }

        if (ret.buyer.id != buyer.id) {
            throw RuntimeException("No autorizado")
        }

        if (ret.status != ReturnStatus.REJECTED) {
            throw RuntimeException("Solo se pueden escalar devoluciones rechazadas")
        }

        ret.status = ReturnStatus.ESCALATED
        ret.escalatedAt = LocalDateTime.now()

        val saved = returnRepository.save(ret)

        notificationService.createAndSend(
            userId = ret.seller.id!!,
            type = NotificationType.ORDER_CANCELLED,
            title = "Devolucion escalada al administrador",
            message = "El comprador ${buyer.name} escalo la devolucion del pedido ${ret.order.orderNumber} al administrador.",
            priority = "high",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun markRefundIssued(id: Long, email: String, request: MarkRefundIssuedRequest): ReturnResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val ret = returnRepository.findById(id)
            .orElseThrow { RuntimeException("Devolucion no encontrada") }

        if (ret.seller.id != seller.id) {
            throw RuntimeException("No autorizado")
        }

        if (ret.status != ReturnStatus.APPROVED) {
            throw RuntimeException("Solo se puede marcar reembolso en devoluciones aprobadas (estado actual: ${ret.status})")
        }

        ret.status = ReturnStatus.REFUND_ISSUED
        ret.refundIssuedAt = LocalDateTime.now()
        ret.refundMethod = request.refundMethod

        val saved = returnRepository.save(ret)

        notificationService.createAndSend(
            userId = ret.buyer.id!!,
            type = NotificationType.PAYMENT_SUCCESS,
            title = "Reembolso enviado",
            message = "La tienda ha enviado tu reembolso de $${ret.refundAmount} por ${request.refundMethod} para el pedido ${ret.order.orderNumber}. Por favor confirma cuando lo recibas.",
            priority = "high",
            actionUrl = "/mis-pedidos",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun confirmRefundReceived(id: Long, email: String): ReturnResponse {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val ret = returnRepository.findById(id)
            .orElseThrow { RuntimeException("Devolucion no encontrada") }

        if (ret.buyer.id != buyer.id) {
            throw RuntimeException("No autorizado")
        }

        if (ret.status != ReturnStatus.REFUND_ISSUED) {
            throw RuntimeException("No se puede confirmar reembolso en estado ${ret.status}")
        }

        ret.status = ReturnStatus.COMPLETED
        ret.refundConfirmedAt = LocalDateTime.now()
        ret.resolvedAt = LocalDateTime.now()

        // Marcar la orden como reembolsada
        val order = ret.order
        order.status = OrderStatus.REFUNDED
        order.paymentStatus = "REFUNDED"
        orderRepository.save(order)

        val saved = returnRepository.save(ret)

        // Notificar a la tienda
        notificationService.createAndSend(
            userId = ret.seller.id!!,
            type = NotificationType.PAYMENT_SUCCESS,
            title = "Reembolso confirmado",
            message = "El comprador ${buyer.name} confirmo la recepcion del reembolso del pedido ${ret.order.orderNumber}.",
            priority = "medium",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    fun autoEscalateUnrespondedReturns() {
        val cutoff = LocalDateTime.now().minusHours(48)
        val pendingReturns = returnRepository.findByStatusAndCreatedAtBefore(ReturnStatus.PENDING, cutoff)

        if (pendingReturns.isEmpty()) return

        logger.info("Auto-escalando ${pendingReturns.size} devoluciones sin respuesta en 48h")

        pendingReturns.forEach { ret ->
            ret.status = ReturnStatus.ESCALATED
            ret.escalatedAt = LocalDateTime.now()
            returnRepository.save(ret)

            notificationService.createAndSend(
                userId = ret.buyer.id!!,
                type = NotificationType.ORDER_CONFIRMED,
                title = "Devolucion escalada automaticamente",
                message = "Tu devolucion del pedido ${ret.order.orderNumber} fue escalada al administrador porque la tienda no respondio en 48 horas.",
                priority = "high",
                actionUrl = "/mis-pedidos",
                relatedEntityId = ret.id,
                relatedEntityType = RelatedEntityType.ORDER
            )

            notificationService.createAndSend(
                userId = ret.seller.id!!,
                type = NotificationType.ORDER_CANCELLED,
                title = "Devolucion escalada por inactividad",
                message = "La devolucion del pedido ${ret.order.orderNumber} fue escalada al administrador por no responder en 48 horas.",
                priority = "high",
                actionUrl = "/admin/orders",
                relatedEntityId = ret.id,
                relatedEntityType = RelatedEntityType.ORDER
            )
        }
    }

    private fun ReturnRequest.toResponse(): ReturnResponse = ReturnResponse(
        id = id!!,
        orderId = order.id!!,
        orderNumber = order.orderNumber,
        buyerId = buyer.id!!,
        buyerName = buyer.name,
        buyerEmail = buyer.email,
        reason = reason,
        description = description,
        imageUrls = imageUrls.toList(),
        status = status,
        requestedSolution = requestedSolution?.name,
        storeResponse = storeResponse,
        adminNotes = adminNotes,
        requiresProductReturn = requiresProductReturn,
        refundAmount = refundAmount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        resolvedAt = resolvedAt,
        escalatedAt = escalatedAt,
        refundIssuedAt = refundIssuedAt,
        refundConfirmedAt = refundConfirmedAt,
        refundMethod = refundMethod
    )
}
