package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.model.PaymentMethod
import com.kompralo.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WompiWebhookService(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun processEvent(payload: Map<String, Any?>) {
        val event = payload["event"] as? String ?: return
        if (event != "transaction.updated") return

        @Suppress("UNCHECKED_CAST")
        val data = payload["data"] as? Map<String, Any?> ?: return
        @Suppress("UNCHECKED_CAST")
        val transaction = data["transaction"] as? Map<String, Any?> ?: return

        val transactionId = transaction["id"] as? String ?: return
        val status = transaction["status"] as? String ?: return

        val order = orderRepository.findByWompiTransactionId(transactionId)
            ?: throw ResourceNotFoundException("Orden no encontrada para transaccion $transactionId")

        when (status) {
            "APPROVED" -> {
                if (order.paymentStatus != "PAID") {
                    val method = order.paymentMethod ?: PaymentMethod.CREDIT_CARD
                    order.markAsPaid(method)
                    orderRepository.save(order)
                }
            }
            "DECLINED", "VOIDED", "ERROR" -> {
                if (order.paymentStatus == "PENDING") {
                    order.paymentStatus = "FAILED"
                    orderRepository.save(order)
                }
            }
        }
    }
}
