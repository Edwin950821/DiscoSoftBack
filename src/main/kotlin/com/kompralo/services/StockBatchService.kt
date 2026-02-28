package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class StockBatchService(
    private val stockBatchRepository: StockBatchRepository,
    private val stockRestockRepository: StockRestockRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val inventoryMovementRepository: InventoryMovementRepository,
    private val notificationService: NotificationService,
    private val supplierRepository: SupplierRepository,
) {

    @Transactional
    fun createBatch(sellerId: Long, request: BatchRestockRequest): BatchResponse {
        if (request.items.isEmpty()) {
            throw RuntimeException("El lote debe tener al menos un producto")
        }

        val seller = userRepository.findById(sellerId)
            .orElseThrow { RuntimeException("Vendedor no encontrado") }

        val itemsWithProduct = request.items.map { item ->
            if (item.quantity <= 0) {
                throw RuntimeException("La cantidad debe ser mayor a 0")
            }
            val product = productRepository.findById(item.productId)
                .orElseThrow { RuntimeException("Producto ${item.productId} no encontrado") }
            if (product.seller.id != sellerId) {
                throw RuntimeException("No autorizado para reabastecer producto ${product.name}")
            }
            Triple(product, item.quantity, item)
        }

        val totalQuantity = itemsWithProduct.sumOf { it.second }
        val totalValue = itemsWithProduct.sumOf { (product, qty, _) ->
            product.price.multiply(BigDecimal(qty))
        }

        val batchNumber = generateBatchNumber()

        // Resolve supplier entity if supplierId provided
        val supplierEntity = request.supplierId?.let { sid ->
            val s = supplierRepository.findById(sid)
                .orElseThrow { RuntimeException("Proveedor no encontrado") }
            if (s.seller.id != sellerId) {
                throw RuntimeException("No autorizado para usar este proveedor")
            }
            s
        }

        val batch = stockBatchRepository.save(
            StockBatch(
                batchNumber = batchNumber,
                seller = seller,
                location = request.location,
                notes = request.notes,
                supplier = supplierEntity?.name ?: request.supplier,
                supplierEntity = supplierEntity,
                totalItems = itemsWithProduct.size,
                totalQuantity = totalQuantity,
                totalValue = totalValue,
            )
        )

        val itemResponses = itemsWithProduct.map { (product, quantity, itemReq) ->
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
                    batch = batch,
                    unitCost = itemReq.unitCost ?: BigDecimal.ZERO,
                    expiryDate = itemReq.expiryDate?.let { LocalDate.parse(it) },
                    quantityRemaining = quantity,
                )
            )

            // Record ENTRY movement
            inventoryMovementRepository.save(
                InventoryMovement(
                    product = product,
                    restock = restock,
                    movementType = "ENTRY",
                    quantity = quantity,
                    resultingStock = product.stock,
                    user = seller,
                    referenceType = "BATCH",
                    referenceId = batch.id,
                    notes = "Entrada - Lote $batchNumber",
                )
            )

            BatchItemResponse(
                productId = product.id!!,
                productName = product.name,
                productSku = product.sku,
                productImageUrl = product.imageUrl,
                quantity = restock.quantity,
                previousStock = restock.previousStock,
                newStock = restock.newStock,
                unitCost = restock.unitCost,
                expiryDate = restock.expiryDate?.toString(),
                quantityRemaining = restock.quantityRemaining,
                quantitySold = restock.quantitySold,
                quantityDamaged = restock.quantityDamaged,
                quantityReturned = restock.quantityReturned,
            )
        }

        try {
            notificationService.createAndSend(
                userId = sellerId,
                type = NotificationType.STOCK_RESTOCKED,
                title = "Lote de stock guardado",
                message = "Lote #$batchNumber: ${itemsWithProduct.size} productos actualizados, $totalQuantity unidades agregadas.",
                priority = "low",
                actionUrl = "/admin/inventory",
                relatedEntityId = batch.id,
                relatedEntityType = RelatedEntityType.PRODUCT,
            )
        } catch (e: Exception) {
            println("[StockBatch] Error enviando notificación: ${e.message}")
        }

        return BatchResponse(
            id = batch.id!!,
            batchNumber = batch.batchNumber,
            location = batch.location,
            notes = batch.notes,
            supplier = batch.supplier,
            supplierId = batch.supplierEntity?.id,
            status = batch.status,
            totalItems = batch.totalItems,
            totalQuantity = batch.totalQuantity,
            totalValue = batch.totalValue,
            createdByName = seller.name,
            createdAt = batch.createdAt,
            items = itemResponses,
        )
    }

    @Transactional(readOnly = true)
    fun getBatches(sellerId: Long, days: Int?): List<BatchSummaryResponse> {
        val batches = if (days != null && days > 0) {
            val after = LocalDateTime.now().minusDays(days.toLong())
            stockBatchRepository.findBySellerIdAndCreatedAtAfterOrderByCreatedAtDesc(sellerId, after)
        } else {
            stockBatchRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
        }

        return batches.map { it.toSummary() }
    }

    @Transactional(readOnly = true)
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
                newStock = it.newStock,
                unitCost = it.unitCost,
                expiryDate = it.expiryDate?.toString(),
                quantityRemaining = it.quantityRemaining,
                quantitySold = it.quantitySold,
                quantityDamaged = it.quantityDamaged,
                quantityReturned = it.quantityReturned,
            )
        }

        return BatchResponse(
            id = batch.id!!,
            batchNumber = batch.batchNumber,
            location = batch.location,
            notes = batch.notes,
            supplier = batch.supplier,
            supplierId = batch.supplierEntity?.id,
            status = batch.status,
            totalItems = batch.totalItems,
            totalQuantity = batch.totalQuantity,
            totalValue = batch.totalValue,
            createdByName = batch.seller.name,
            createdAt = batch.createdAt,
            items = items,
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
        supplier = supplier,
        supplierId = supplierEntity?.id,
        status = status,
        totalItems = totalItems,
        totalQuantity = totalQuantity,
        totalValue = totalValue,
        createdByName = seller.name,
        createdAt = createdAt,
    )
}
