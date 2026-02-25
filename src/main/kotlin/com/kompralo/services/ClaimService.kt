package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ShippingClaimRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ClaimService(
    private val claimRepository: ShippingClaimRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {

    @Transactional
    fun createClaim(email: String, request: CreateClaimRequest): ClaimResponse {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val order = orderRepository.findById(request.orderId)
            .orElseThrow { RuntimeException("Pedido no encontrado") }

        if (order.buyer.id != buyer.id) {
            throw RuntimeException("No autorizado para reclamar este pedido")
        }

        if (order.status != OrderStatus.SHIPPED) {
            throw RuntimeException("Solo se pueden reclamar pedidos en estado SHIPPED")
        }

        if (claimRepository.existsByOrderId(request.orderId)) {
            throw RuntimeException("Ya existe un reclamo para este pedido")
        }

        val claim = ShippingClaim(
            order = order,
            buyer = buyer,
            seller = order.seller,
            type = request.type,
            estimatedDeliveryDate = order.shippedAt?.plusDays(5)
        )

        val saved = claimRepository.save(claim)

        notificationService.createAndSend(
            userId = order.seller.id!!,
            type = NotificationType.NEW_ORDER,
            title = "Nuevo reclamo por retraso",
            message = "El comprador ${buyer.name} reclamo retraso en el pedido ${order.orderNumber}.",
            priority = "high",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    fun getMyClaims(email: String): List<ClaimResponse> {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        return claimRepository.findByBuyerOrderByCreatedAtDesc(buyer).map { it.toResponse() }
    }

    fun getClaims(email: String, status: ClaimStatus?): List<ClaimResponse> {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val claims = if (status != null) {
            claimRepository.findBySellerAndStatusOrderByCreatedAtDesc(seller, status)
        } else {
            claimRepository.findBySellerOrderByCreatedAtDesc(seller)
        }
        return claims.map { it.toResponse() }
    }

    fun getClaim(id: Long): ClaimResponse {
        val claim = claimRepository.findById(id)
            .orElseThrow { RuntimeException("Reclamo no encontrado") }
        return claim.toResponse()
    }

    fun getClaimStats(email: String): ClaimStatsResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        return ClaimStatsResponse(
            total = claimRepository.countBySeller(seller),
            open = claimRepository.countBySellerAndStatus(seller, ClaimStatus.OPEN),
            inReview = claimRepository.countBySellerAndStatus(seller, ClaimStatus.IN_REVIEW),
            resolved = claimRepository.countBySellerAndStatus(seller, ClaimStatus.RESOLVED),
            closed = claimRepository.countBySellerAndStatus(seller, ClaimStatus.CLOSED),
            extended = claimRepository.countBySellerAndStatus(seller, ClaimStatus.EXTENDED)
        )
    }

    @Transactional
    fun extendClaim(id: Long, email: String): ClaimResponse {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val claim = claimRepository.findById(id)
            .orElseThrow { RuntimeException("Reclamo no encontrado") }

        if (claim.buyer.id != buyer.id) {
            throw RuntimeException("No autorizado")
        }

        claim.status = ClaimStatus.EXTENDED
        claim.resolution = ClaimResolution.EXTENDED
        claim.resolvedAt = LocalDateTime.now()

        val saved = claimRepository.save(claim)

        notificationService.createAndSend(
            userId = claim.seller.id!!,
            type = NotificationType.ORDER_CONFIRMED,
            title = "Reclamo extendido",
            message = "El comprador ${buyer.name} acepto extender el plazo del pedido ${claim.order.orderNumber}.",
            priority = "medium",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun requestRefund(id: Long, email: String): ClaimResponse {
        val buyer = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val claim = claimRepository.findById(id)
            .orElseThrow { RuntimeException("Reclamo no encontrado") }

        if (claim.buyer.id != buyer.id) {
            throw RuntimeException("No autorizado")
        }

        claim.status = ClaimStatus.RESOLVED
        claim.resolution = ClaimResolution.REFUND
        claim.resolvedAt = LocalDateTime.now()

        val order = claim.order
        order.status = OrderStatus.REFUNDED
        order.paymentStatus = "REFUNDED"
        orderRepository.save(order)

        val saved = claimRepository.save(claim)

        notificationService.createAndSend(
            userId = claim.seller.id!!,
            type = NotificationType.ORDER_CANCELLED,
            title = "Reembolso solicitado por reclamo",
            message = "El comprador ${buyer.name} solicito reembolso del pedido ${claim.order.orderNumber}.",
            priority = "urgent",
            actionUrl = "/admin/orders",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        notificationService.createAndSend(
            userId = buyer.id!!,
            type = NotificationType.PAYMENT_SUCCESS,
            title = "Reembolso en proceso",
            message = "Tu reembolso del pedido ${order.orderNumber} esta siendo procesado.",
            priority = "high",
            actionUrl = "/mis-pedidos",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun storeRespond(id: Long, email: String, request: StoreRespondClaimRequest): ClaimResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
        val claim = claimRepository.findById(id)
            .orElseThrow { RuntimeException("Reclamo no encontrado") }

        if (claim.seller.id != seller.id) {
            throw RuntimeException("No autorizado")
        }

        claim.storeResponse = request.response
        claim.storeResponseDate = LocalDateTime.now()
        claim.status = ClaimStatus.IN_REVIEW

        val saved = claimRepository.save(claim)

        notificationService.createAndSend(
            userId = claim.buyer.id!!,
            type = NotificationType.ORDER_CONFIRMED,
            title = "La tienda respondio a tu reclamo",
            message = "La tienda respondio a tu reclamo del pedido ${claim.order.orderNumber}.",
            priority = "medium",
            actionUrl = "/mis-pedidos",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun adminResolve(id: Long, request: AdminResolveClaimRequest): ClaimResponse {
        val claim = claimRepository.findById(id)
            .orElseThrow { RuntimeException("Reclamo no encontrado") }

        val resolution = try {
            ClaimResolution.valueOf(request.resolution.uppercase())
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Resolucion invalida: ${request.resolution}")
        }

        claim.resolution = resolution
        claim.status = ClaimStatus.RESOLVED
        claim.resolvedAt = LocalDateTime.now()

        if (resolution == ClaimResolution.REFUND) {
            val order = claim.order
            order.status = OrderStatus.REFUNDED
            order.paymentStatus = "REFUNDED"
            orderRepository.save(order)
        }

        val saved = claimRepository.save(claim)

        notificationService.createAndSend(
            userId = claim.buyer.id!!,
            type = NotificationType.ORDER_CONFIRMED,
            title = "Reclamo resuelto",
            message = "Tu reclamo del pedido ${claim.order.orderNumber} fue resuelto.",
            priority = "high",
            actionUrl = "/mis-pedidos",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.ORDER
        )

        return saved.toResponse()
    }

    @Transactional
    fun autoEscalateUnrespondedClaims() {
        val deadline = LocalDateTime.now().minusHours(24)
        val unresponded = claimRepository.findUnrespondedBefore(ClaimStatus.OPEN, deadline)

        for (claim in unresponded) {
            claim.status = ClaimStatus.IN_REVIEW
            claim.autoResolved = true
            claimRepository.save(claim)

            notificationService.createAndSend(
                userId = claim.seller.id!!,
                type = NotificationType.ORDER_CANCELLED,
                title = "Reclamo escalado automaticamente",
                message = "El reclamo del pedido ${claim.order.orderNumber} fue escalado por falta de respuesta.",
                priority = "urgent",
                actionUrl = "/admin/orders",
                relatedEntityId = claim.id,
                relatedEntityType = RelatedEntityType.ORDER
            )
        }
    }

    private fun ShippingClaim.toResponse(): ClaimResponse = ClaimResponse(
        id = id!!,
        orderId = order.id!!,
        orderNumber = order.orderNumber,
        buyerId = buyer.id!!,
        buyerName = buyer.name,
        buyerEmail = buyer.email,
        storeId = seller.id!!,
        storeName = seller.name,
        type = type,
        status = status,
        estimatedDeliveryDate = estimatedDeliveryDate,
        claimDate = claimDate,
        storeResponse = storeResponse,
        storeResponseDate = storeResponseDate,
        resolution = resolution,
        resolvedAt = resolvedAt,
        autoResolved = autoResolved,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
