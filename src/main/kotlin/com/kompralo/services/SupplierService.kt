package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.*
import com.kompralo.model.Supplier
import com.kompralo.repository.StockBatchRepository
import com.kompralo.repository.StockRestockRepository
import com.kompralo.repository.SupplierRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class SupplierService(
    private val supplierRepository: SupplierRepository,
    private val stockBatchRepository: StockBatchRepository,
    private val stockRestockRepository: StockRestockRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createSupplier(email: String, request: CreateSupplierRequest): SupplierResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuario", email) }

        if (request.name.isBlank()) {
            throw ValidationException("El nombre del proveedor es obligatorio")
        }

        if (!request.nit.isNullOrBlank() && supplierRepository.existsBySellerIdAndNit(seller.id!!, request.nit)) {
            throw ResourceAlreadyExistsException("Ya existe un proveedor con ese NIT")
        }

        val supplier = supplierRepository.save(
            Supplier(
                seller = seller,
                name = request.name.trim(),
                nit = request.nit?.trim(),
                contactName = request.contactName?.trim(),
                email = request.email?.trim(),
                phone = request.phone?.trim(),
                address = request.address?.trim(),
                city = request.city?.trim(),
                notes = request.notes?.trim()
            )
        )

        return supplier.toResponse()
    }

    @Transactional
    fun updateSupplier(email: String, supplierId: Long, request: UpdateSupplierRequest): SupplierResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuario", email) }

        val supplier = supplierRepository.findById(supplierId)
            .orElseThrow { EntityNotFoundException("Proveedor", supplierId) }

        if (supplier.seller.id != seller.id) {
            throw UnauthorizedActionException("No autorizado")
        }

        request.name?.let { supplier.name = it.trim() }
        request.nit?.let { nit ->
            if (nit.isNotBlank() && supplierRepository.existsBySellerIdAndNitAndIdNot(seller.id!!, nit, supplierId)) {
                throw ResourceAlreadyExistsException("Ya existe un proveedor con ese NIT")
            }
            supplier.nit = nit.trim()
        }
        request.contactName?.let { supplier.contactName = it.trim() }
        request.email?.let { supplier.email = it.trim() }
        request.phone?.let { supplier.phone = it.trim() }
        request.address?.let { supplier.address = it.trim() }
        request.city?.let { supplier.city = it.trim() }
        request.notes?.let { supplier.notes = it.trim() }

        return supplierRepository.save(supplier).toResponse()
    }

    @Transactional
    fun deactivateSupplier(email: String, supplierId: Long): SupplierResponse {
        val supplier = getOwnedSupplier(email, supplierId)
        supplier.isActive = false
        return supplierRepository.save(supplier).toResponse()
    }

    @Transactional
    fun activateSupplier(email: String, supplierId: Long): SupplierResponse {
        val supplier = getOwnedSupplier(email, supplierId)
        supplier.isActive = true
        return supplierRepository.save(supplier).toResponse()
    }

    @Transactional(readOnly = true)
    fun getSuppliers(email: String, includeInactive: Boolean = false): List<SupplierResponse> {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuario", email) }

        val suppliers = if (includeInactive) {
            supplierRepository.findBySellerIdOrderByNameAsc(seller.id!!)
        } else {
            supplierRepository.findBySellerIdAndIsActiveTrueOrderByNameAsc(seller.id!!)
        }

        return suppliers.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getSupplier(email: String, supplierId: Long): SupplierResponse {
        return getOwnedSupplier(email, supplierId).toResponse()
    }

    @Transactional(readOnly = true)
    fun getSupplierSummaries(email: String): List<SupplierSummaryResponse> {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuario", email) }

        return supplierRepository.findBySellerIdAndIsActiveTrueOrderByNameAsc(seller.id!!).map {
            SupplierSummaryResponse(
                id = it.id!!,
                name = it.name,
                nit = it.nit,
                isActive = it.isActive
            )
        }
    }

    @Transactional(readOnly = true)
    fun getSupplierStats(email: String): SupplierStatsResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuario", email) }

        val sellerId = seller.id!!
        val total = supplierRepository.countBySellerId(sellerId).toInt()
        val active = supplierRepository.countBySellerIdAndIsActiveTrue(sellerId).toInt()
        val batchCount = stockBatchRepository.countBySellerIdAndSupplierEntityIsNotNull(sellerId)

        val batches = stockBatchRepository.findBySellerIdAndSupplierEntityIsNotNull(sellerId)
        val totalValue = batches.sumOf { it.totalValue }

        return SupplierStatsResponse(
            totalSuppliers = total,
            activeSuppliers = active,
            totalPurchaseValue = totalValue,
            totalBatches = batchCount.toInt()
        )
    }

    @Transactional(readOnly = true)
    fun getSupplierMetrics(email: String): List<SupplierMetricResponse> {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuario", email) }

        val batches = stockBatchRepository.findBySellerIdAndSupplierEntityIsNotNull(seller.id!!)

        val grouped = batches.groupBy { it.supplierEntity!!.id!! }

        return grouped.map { (supplierId, supplierBatches) ->
            val supplierName = supplierBatches.first().supplierEntity!!.name
            val totalQuantity = supplierBatches.sumOf { it.totalQuantity }
            val totalValue = supplierBatches.sumOf { it.totalValue }
            val avgBatchValue = if (supplierBatches.isNotEmpty()) {
                totalValue.divide(BigDecimal(supplierBatches.size), 2, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            val lastPurchase = supplierBatches.maxByOrNull { it.createdAt }?.createdAt
            val batchIds = supplierBatches.mapNotNull { it.id }
            val productCount = if (batchIds.isNotEmpty()) {
                stockRestockRepository.countDistinctProductsByBatchIds(batchIds).toInt()
            } else 0

            SupplierMetricResponse(
                supplierId = supplierId,
                supplierName = supplierName,
                totalBatches = supplierBatches.size,
                totalQuantity = totalQuantity,
                totalValue = totalValue,
                avgBatchValue = avgBatchValue,
                lastPurchaseDate = lastPurchase,
                productCount = productCount
            )
        }.sortedByDescending { it.totalValue }
    }

    @Transactional(readOnly = true)
    fun getSupplierPurchaseHistory(email: String, supplierId: Long): List<SupplierPurchaseHistoryResponse> {
        getOwnedSupplier(email, supplierId) // validate ownership

        val batches = stockBatchRepository.findBySupplierEntityIdOrderByCreatedAtDesc(supplierId)

        return batches.map {
            SupplierPurchaseHistoryResponse(
                batchId = it.id!!,
                batchNumber = it.batchNumber,
                totalItems = it.totalItems,
                totalQuantity = it.totalQuantity,
                totalValue = it.totalValue,
                createdAt = it.createdAt
            )
        }
    }

    private fun getOwnedSupplier(email: String, supplierId: Long): Supplier {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Usuario", email) }

        val supplier = supplierRepository.findById(supplierId)
            .orElseThrow { EntityNotFoundException("Proveedor", supplierId) }

        if (supplier.seller.id != seller.id) {
            throw UnauthorizedActionException("No autorizado")
        }

        return supplier
    }

    private fun Supplier.toResponse() = SupplierResponse(
        id = id!!,
        name = name,
        nit = nit,
        contactName = contactName,
        email = email,
        phone = phone,
        address = address,
        city = city,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
