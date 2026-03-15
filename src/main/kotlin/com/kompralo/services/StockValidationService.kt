package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.CheckoutItemRequest
import com.kompralo.model.Product
import com.kompralo.model.ProductStatus
import com.kompralo.model.ProductVariant
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.ProductVariantRepository
import org.springframework.stereotype.Service

data class ValidatedCart(
    val productMap: Map<Long, Product>,
    val variantMap: Map<Long, ProductVariant>
)

@Service
class StockValidationService(
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository
) {

    fun validateAndLoad(items: List<CheckoutItemRequest>): ValidatedCart {
        if (items.isEmpty()) {
            throw ValidationException("El carrito está vacío")
        }

        val productIds = items.map { it.productId }
        val products = productRepository.findAllByIdWithSeller(productIds)
        val productMap = products.associateBy { it.id!! }
        val variantMap = mutableMapOf<Long, ProductVariant>()

        for (item in items) {
            if (item.quantity <= 0) {
                throw ValidationException("La cantidad debe ser mayor a 0")
            }

            val product = productMap[item.productId]
                ?: throw EntityNotFoundException("Producto", item.productId)

            if (product.status != ProductStatus.ACTIVE) {
                throw BusinessRuleViolationException("El producto '${product.name}' no está disponible")
            }

            if (item.variantId != null) {
                val variant = productVariantRepository.findById(item.variantId)
                    .orElseThrow { EntityNotFoundException("Variante", item.variantId) }
                if (variant.product.id != product.id) {
                    throw BusinessRuleViolationException("La variante no pertenece al producto '${product.name}'")
                }
                if (!variant.active) {
                    throw BusinessRuleViolationException("La variante '${variant.name}' no está disponible")
                }
                if (variant.stock < item.quantity) {
                    throw InsufficientStockException("${product.name} - ${variant.name}", item.quantity, variant.stock)
                }
                variantMap[item.variantId] = variant
            } else {
                if (product.stock < item.quantity) {
                    throw InsufficientStockException(product.name, item.quantity, product.stock)
                }
            }
        }

        return ValidatedCart(productMap, variantMap)
    }
}
