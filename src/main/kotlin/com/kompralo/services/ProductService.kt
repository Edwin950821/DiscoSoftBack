package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.StockRestockRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val stockRestockRepository: StockRestockRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {

    companion object {
        const val LOW_STOCK_THRESHOLD = 10
    }

    fun getProductsBySeller(sellerId: Long): List<ProductResponse> {
        return productRepository.findBySellerId(sellerId).map { it.toResponse() }
    }

    fun getProductById(id: Long, sellerId: Long): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { RuntimeException("Producto no encontrado") }
        if (product.seller.id != sellerId) {
            throw RuntimeException("No autorizado para ver este producto")
        }
        return product.toResponse()
    }

    fun searchProducts(sellerId: Long, search: String): List<ProductResponse> {
        return productRepository.searchBySellerAndName(sellerId, search).map { it.toResponse() }
    }

    @Transactional
    fun createProduct(sellerId: Long, request: CreateProductRequest): ProductResponse {
        val seller = userRepository.findById(sellerId)
            .orElseThrow { RuntimeException("Vendedor no encontrado") }

        if (productRepository.existsBySku(request.sku)) {
            throw RuntimeException("El SKU '${request.sku}' ya existe")
        }

        val product = Product(
            seller = seller,
            name = request.name,
            sku = request.sku,
            category = request.category,
            price = request.price,
            stock = request.stock,
            imageUrl = request.imageUrl,
            description = request.description,
            status = if (request.stock > 0) ProductStatus.ACTIVE else ProductStatus.OUT_OF_STOCK
        )

        val saved = productRepository.save(product)
        notificationService.createAndSend(
            userId = sellerId,
            type = NotificationType.PRODUCT_CREATED,
            title = "Producto creado",
            message = "El producto '${saved.name}' ha sido creado exitosamente.",
            priority = "low",
            actionUrl = "/admin/products",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.PRODUCT
        )
        return saved.toResponse()
    }

    @Transactional
    fun updateProduct(id: Long, sellerId: Long, request: UpdateProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            .orElseThrow { RuntimeException("Producto no encontrado") }

        if (product.seller.id != sellerId) {
            throw RuntimeException("No autorizado para editar este producto")
        }

        request.name?.let { product.name = it }
        request.sku?.let { sku ->
            val existing = productRepository.findBySku(sku)
            if (existing != null && existing.id != id) {
                throw RuntimeException("El SKU '$sku' ya está en uso")
            }
            product.sku = sku
        }
        request.category?.let { product.category = it }
        request.price?.let { product.price = it }
        request.stock?.let {
            product.stock = it
            product.updateStockStatus()
        }
        request.imageUrl?.let { product.imageUrl = it }
        request.description?.let { product.description = it }
        request.status?.let { product.status = ProductStatus.valueOf(it) }

        val saved = productRepository.save(product)
        notificationService.createAndSend(
            userId = sellerId,
            type = NotificationType.PRODUCT_UPDATED,
            title = "Producto actualizado",
            message = "El producto '${saved.name}' ha sido actualizado.",
            priority = "low",
            actionUrl = "/admin/products",
            relatedEntityId = saved.id,
            relatedEntityType = RelatedEntityType.PRODUCT
        )
        if (saved.stock <= LOW_STOCK_THRESHOLD) {
            notificationService.createAndSend(
                userId = sellerId,
                type = NotificationType.LOW_STOCK,
                title = "Stock bajo",
                message = "El producto '${saved.name}' tiene solo ${saved.stock} unidades.",
                priority = "high",
                actionUrl = "/admin/products",
                relatedEntityId = saved.id,
                relatedEntityType = RelatedEntityType.PRODUCT
            )
        }
        return saved.toResponse()
    }

    @Transactional
    fun deleteProduct(id: Long, sellerId: Long) {
        val product = productRepository.findById(id)
            .orElseThrow { RuntimeException("Producto no encontrado") }

        if (product.seller.id != sellerId) {
            throw RuntimeException("No autorizado para eliminar este producto")
        }

        val productName = product.name
        val productId = product.id
        productRepository.delete(product)
        notificationService.createAndSend(
            userId = sellerId,
            type = NotificationType.PRODUCT_DELETED,
            title = "Producto eliminado",
            message = "El producto '$productName' ha sido eliminado.",
            priority = "low",
            relatedEntityId = productId,
            relatedEntityType = RelatedEntityType.PRODUCT
        )
    }

    @Transactional
    fun restockProduct(sellerId: Long, request: RestockRequest): RestockResponse {
        if (request.quantity <= 0) {
            throw RuntimeException("La cantidad debe ser mayor a 0")
        }

        val product = productRepository.findById(request.productId)
            .orElseThrow { RuntimeException("Producto no encontrado") }

        if (product.seller.id != sellerId) {
            throw RuntimeException("No autorizado para reabastecer este producto")
        }

        val seller = userRepository.findById(sellerId)
            .orElseThrow { RuntimeException("Vendedor no encontrado") }

        val previousStock = product.stock
        product.restock(request.quantity)
        productRepository.save(product)

        val restock = StockRestock(
            product = product,
            quantity = request.quantity,
            previousStock = previousStock,
            newStock = product.stock,
            restockDate = request.restockDate,
            notes = request.notes,
            createdBy = seller
        )

        val saved = stockRestockRepository.save(restock)

        notificationService.createAndSend(
            userId = sellerId,
            type = NotificationType.STOCK_RESTOCKED,
            title = "Stock reabastecido",
            message = "Se agregaron ${request.quantity} unidades a '${product.name}'. Stock: $previousStock -> ${product.stock}.",
            priority = "low",
            actionUrl = "/admin/products",
            relatedEntityId = product.id,
            relatedEntityType = RelatedEntityType.PRODUCT
        )
        if (product.stock <= LOW_STOCK_THRESHOLD) {
            notificationService.createAndSend(
                userId = sellerId,
                type = NotificationType.LOW_STOCK,
                title = "Stock bajo",
                message = "El producto '${product.name}' tiene solo ${product.stock} unidades.",
                priority = "high",
                actionUrl = "/admin/products",
                relatedEntityId = product.id,
                relatedEntityType = RelatedEntityType.PRODUCT
            )
        }

        return RestockResponse(
            id = saved.id!!,
            productId = product.id!!,
            productName = product.name,
            quantity = saved.quantity,
            previousStock = saved.previousStock,
            newStock = saved.newStock,
            restockDate = saved.restockDate,
            notes = saved.notes,
            createdAt = saved.createdAt
        )
    }

    fun getRestockHistory(productId: Long, sellerId: Long): List<RestockResponse> {
        val product = productRepository.findById(productId)
            .orElseThrow { RuntimeException("Producto no encontrado") }

        if (product.seller.id != sellerId) {
            throw RuntimeException("No autorizado")
        }

        return stockRestockRepository.findByProductIdOrderByCreatedAtDesc(productId).map {
            RestockResponse(
                id = it.id!!,
                productId = it.product.id!!,
                productName = it.product.name,
                quantity = it.quantity,
                previousStock = it.previousStock,
                newStock = it.newStock,
                restockDate = it.restockDate,
                notes = it.notes,
                createdAt = it.createdAt
            )
        }
    }

    private fun Product.toResponse() = ProductResponse(
        id = id!!,
        name = name,
        sku = sku,
        category = category,
        price = price,
        stock = stock,
        sales = sales,
        status = status.name,
        imageUrl = imageUrl,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
