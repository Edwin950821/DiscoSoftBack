package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.StockBatchRepository
import com.kompralo.repository.StockRestockRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class StockBatchService(
    private val stockBatchRepository: StockBatchRepository,
    private val stockRestockRepository: StockRestockRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {

    @Transactional
    fun createBatch(sellerId: Long, request: BatchRestockRequest): BatchResponse {
        if (request.items.isEmpty()) {
            throw RuntimeException("El lote debe tener al menos un producto")
        }

        val seller = userRepository.findById(sellerId)
            .orElseThrow { RuntimeException("Vendedor no encontrado") }

        val products = request.items.map { item ->
            if (item.quantity <= 0) {
                throw RuntimeException("La cantidad debe ser mayor a 0")
            }
            val product = productRepository.findById(item.productId)
                .orElseThrow { RuntimeException("Producto ${item.productId} no encontrado") }
            if (product.seller.id != sellerId) {
                throw RuntimeException("No autorizado para reabastecer producto ${product.name}")
            }
            product to item.quantity
        }

        val totalQuantity = products.sumOf { it.second }
        val totalValue = products.sumOf { (product, qty) ->
            product.price.multiply(java.math.BigDecimal(qty))
        }

        val batchNumber = generateBatchNumber()

        val batch = stockBatchRepository.save(
            StockBatch(
                batchNumber = batchNumber,
                seller = seller,
                location = request.location,
                notes = request.notes,
                totalItems = products.size,
                totalQuantity = totalQuantity,
                totalValue = totalValue
            )
        )

        val itemResponses = products.map { (product, quantity) ->
            val previousStock = product.stock
            product.restock(quantity)
            productRepository.save(product)

            val restock = stockRestockRepository.save(
                StockRestock(
                    product = product,
                    quantity = quantity,
                    previousStock = previousStock,
                    newStock = product.stock,
                    restockDate = LocalDate.now(),
                    notes = "Lote: $batchNumber",
                    createdBy = seller,
                    batch = batch
                )
            )

            BatchItemResponse(
                productId = product.id!!,
                productName = product.name,
                productSku = product.sku,
                productImageUrl = product.imageUrl,
                quantity = restock.quantity,
                previousStock = restock.previousStock,
                newStock = restock.newStock
            )
        }

        notificationService.createAndSend(
            userId = sellerId,
            type = NotificationType.STOCK_RESTOCKED,
            title = "Lote de stock guardado",
            message = "Lote #$batchNumber: ${products.size} productos actualizados, $totalQuantity unidades agregadas.",
            priority = "low",
            actionUrl = "/admin/inventory",
            relatedEntityId = batch.id,
            relatedEntityType = RelatedEntityType.PRODUCT
        )

        return BatchResponse(
            id = batch.id!!,
            batchNumber = batch.batchNumber,
            location = batch.location,
            notes = batch.notes,
            totalItems = batch.totalItems,
            totalQuantity = batch.totalQuantity,
            totalValue = batch.totalValue,
            createdByName = seller.name,
            createdAt = batch.createdAt,
            items = itemResponses
        )
    }

    fun getBatches(sellerId: Long, days: Int?): List<BatchSummaryResponse> {
        val batches = if (days != null && days > 0) {
            val after = LocalDateTime.now().minusDays(days.toLong())
            stockBatchRepository.findBySellerIdAndCreatedAtAfterOrderByCreatedAtDesc(sellerId, after)
        } else {
            stockBatchRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
        }

        return batches.map { it.toSummary() }
    }

    fun getBatchById(sellerId: Long, batchId: Long): BatchResponse {
        val batch = stockBatchRepository.findById(batchId)
            .orElseThrow { RuntimeException("Lote no encontrado") }

        if (batch.seller.id != sellerId) {
            throw RuntimeException("No autorizado")
        }

        val restocks = stockRestockRepository.findByBatchIdOrderByCreatedAtDesc(batchId)
        val items = restocks.map {
            BatchItemResponse(
                productId = it.product.id!!,
                productName = it.product.name,
                productSku = it.product.sku,
                productImageUrl = it.product.imageUrl,
                quantity = it.quantity,
                previousStock = it.previousStock,
                newStock = it.newStock
            )
        }

        return BatchResponse(
            id = batch.id!!,
            batchNumber = batch.batchNumber,
            location = batch.location,
            notes = batch.notes,
            totalItems = batch.totalItems,
            totalQuantity = batch.totalQuantity,
            totalValue = batch.totalValue,
            createdByName = batch.seller.name,
            createdAt = batch.createdAt,
            items = items
        )
    }

    private fun generateBatchNumber(): String {
        val random = (10000..99999).random()
        return "KMP-$random-SU"
    }

    private fun StockBatch.toSummary() = BatchSummaryResponse(
        id = id!!,
        batchNumber = batchNumber,
        location = location,
        totalItems = totalItems,
        totalQuantity = totalQuantity,
        totalValue = totalValue,
        createdByName = seller.name,
        createdAt = createdAt
    )
}
