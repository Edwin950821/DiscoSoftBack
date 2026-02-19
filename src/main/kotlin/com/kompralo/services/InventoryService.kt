package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class InventoryService(
    private val stockRestockRepository: StockRestockRepository,
    private val inventoryMovementRepository: InventoryMovementRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
) {

    @Transactional(readOnly = true)
    fun getInventoryItems(email: String, search: String?, status: String?): List<InventoryItemResponse> {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val restocks = if (!search.isNullOrBlank()) {
            stockRestockRepository.searchBySellerAndText(seller.id!!, search)
        } else {
            stockRestockRepository.findBySellerIdWithProduct(seller.id!!)
        }

        return restocks
            .map { it.toInventoryItemResponse() }
            .let { items ->
                if (status.isNullOrBlank()) items
                else items.filter { it.status == status }
            }
    }

    @Transactional(readOnly = true)
    fun getInventoryItem(email: String, restockId: Long): InventoryItemResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val restock = stockRestockRepository.findById(restockId)
            .orElseThrow { RuntimeException("Registro de inventario no encontrado") }

        if (restock.product.seller.id != seller.id) {
            throw RuntimeException("No autorizado")
        }

        return restock.toInventoryItemResponse()
    }

    @Transactional
    fun adjustStock(email: String, request: AdjustStockRequest): InventoryItemResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val restock = stockRestockRepository.findById(request.restockId)
            .orElseThrow { RuntimeException("Registro de inventario no encontrado") }

        if (restock.product.seller.id != seller.id) {
            throw RuntimeException("No autorizado")
        }

        if (request.quantity == 0) {
            throw RuntimeException("La cantidad de ajuste no puede ser 0")
        }

        val product = restock.product

        // For negative adjustments, verify we have enough stock
        if (request.quantity < 0) {
            val absQty = -request.quantity
            if (restock.quantityRemaining < absQty) {
                throw RuntimeException("Stock insuficiente en este lote. Disponible: ${restock.quantityRemaining}")
            }
            restock.quantityRemaining -= absQty

            // Track reason
            when (request.reason.uppercase()) {
                "DAMAGE" -> restock.quantityDamaged += absQty
                "RETURN" -> restock.quantityReturned += absQty
            }

            product.stock -= absQty
        } else {
            restock.quantityRemaining += request.quantity
            product.stock += request.quantity
        }

        product.updateStockStatus()
        productRepository.save(product)
        stockRestockRepository.save(restock)

        // Record movement
        inventoryMovementRepository.save(
            InventoryMovement(
                product = product,
                restock = restock,
                movementType = "ADJUSTMENT",
                quantity = request.quantity,
                resultingStock = product.stock,
                user = seller,
                referenceType = "MANUAL",
                reason = request.reason,
                notes = request.notes,
            )
        )

        // Check for low stock alerts
        if (product.stock in 1..10) {
            try {
                notificationService.createAndSend(
                    userId = seller.id!!,
                    type = NotificationType.LOW_STOCK,
                    title = "Stock bajo",
                    message = "${product.name} tiene solo ${product.stock} unidades.",
                    priority = "medium",
                    actionUrl = "/admin/inventory",
                    relatedEntityId = product.id,
                    relatedEntityType = RelatedEntityType.PRODUCT,
                )
            } catch (e: Exception) {
                println("[Inventory] Error enviando notificación: ${e.message}")
            }
        }

        if (product.stock <= 0) {
            try {
                notificationService.createAndSend(
                    userId = seller.id!!,
                    type = NotificationType.OUT_OF_STOCK,
                    title = "Producto agotado",
                    message = "${product.name} se agotó.",
                    priority = "high",
                    actionUrl = "/admin/inventory",
                    relatedEntityId = product.id,
                    relatedEntityType = RelatedEntityType.PRODUCT,
                )
            } catch (e: Exception) {
                println("[Inventory] Error enviando notificación: ${e.message}")
            }
        }

        return restock.toInventoryItemResponse()
    }

    @Transactional(readOnly = true)
    fun getMovements(
        email: String,
        productId: Long?,
        page: Int,
        size: Int,
    ): Map<String, Any> {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

        val pageable = PageRequest.of(page, size)

        val movementPage = if (productId != null) {
            inventoryMovementRepository.findBySellerAndProductPaged(seller.id!!, productId, pageable)
        } else {
            inventoryMovementRepository.findBySellerIdPaged(seller.id!!, pageable)
        }

        return mapOf(
            "content" to movementPage.content.map { it.toResponse() },
            "totalElements" to movementPage.totalElements,
            "totalPages" to movementPage.totalPages,
        )
    }

    private fun StockRestock.toInventoryItemResponse(): InventoryItemResponse {
        val prod = this.product
        val batchNum = try { this.batch?.batchNumber } catch (_: Exception) { null } ?: "N/A"
        val loc = try { this.batch?.location } catch (_: Exception) { null }
        val sup = try { this.batch?.supplier } catch (_: Exception) { null }
        val uc = this.unitCost ?: BigDecimal.ZERO
        val sp = prod.price
        val tc = uc.multiply(BigDecimal(this.quantity))
        val margin = sp.subtract(uc)
        val marginPct = if (uc.compareTo(BigDecimal.ZERO) > 0) {
            margin.divide(uc, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
                .setScale(1, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        val status = when {
            this.expiryDate != null && this.expiryDate!!.isBefore(LocalDate.now()) -> "EXPIRED"
            this.quantityRemaining <= 0 -> "DEPLETED"
            this.quantityRemaining <= (this.quantity * 0.2).toInt() -> "LOW"
            else -> "ACTIVE"
        }

        return InventoryItemResponse(
            id = this.id!!,
            batchNumber = batchNum,
            productId = prod.id!!,
            productName = prod.name,
            productSku = prod.sku,
            productImageUrl = prod.imageUrl,
            quantityReceived = this.quantity,
            quantityRemaining = this.quantityRemaining,
            quantitySold = this.quantitySold,
            quantityDamaged = this.quantityDamaged,
            quantityReturned = this.quantityReturned,
            unitCost = uc,
            sellingPrice = sp,
            totalCost = tc,
            margin = margin,
            marginPct = marginPct,
            expiryDate = this.expiryDate?.toString(),
            location = loc,
            supplier = sup,
            status = status,
            createdAt = this.createdAt.toString(),
        )
    }

    private fun InventoryMovement.toResponse() = InventoryMovementResponse(
        id = this.id!!,
        productId = this.product.id!!,
        productName = this.product.name,
        productSku = this.product.sku,
        movementType = this.movementType,
        quantity = this.quantity,
        resultingStock = this.resultingStock,
        userName = this.user.name,
        referenceType = this.referenceType,
        referenceId = this.referenceId,
        reason = this.reason,
        notes = this.notes,
        createdAt = this.createdAt.toString(),
    )
}
