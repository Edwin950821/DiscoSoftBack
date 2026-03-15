package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.CustomerResponse
import com.kompralo.dto.CustomerStatsResponse
import com.kompralo.model.User
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.UserProfileRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class CustomerService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository
) {

    companion object {
        private val VIP_SPENT_THRESHOLD = BigDecimal(500_000)
        private const val VIP_ORDERS_THRESHOLD = 10L
        private const val FREQUENT_ORDERS_THRESHOLD = 5L
        private const val NEW_DAYS = 30L
        private const val INACTIVE_DAYS = 90L
    }

    fun getCustomersBySeller(
        sellerEmail: String,
        search: String? = null,
        segment: String? = null
    ): List<CustomerResponse> {
        val seller = findSeller(sellerEmail)
        val buyers = orderRepository.findDistinctBuyersBySeller(seller)

        var customers = buyers.map { buyer -> buildCustomerResponse(buyer, seller) }

        if (!search.isNullOrBlank()) {
            val searchLower = search.lowercase()
            customers = customers.filter { c ->
                c.name.lowercase().contains(searchLower) ||
                c.email.lowercase().contains(searchLower) ||
                (c.phone?.lowercase()?.contains(searchLower) == true) ||
                (c.city?.lowercase()?.contains(searchLower) == true)
            }
        }

        if (!segment.isNullOrBlank()) {
            customers = customers.filter { c -> c.segment == segment }
        }

        return customers.sortedByDescending { it.totalSpent }
    }

    fun getCustomerById(customerId: Long, sellerEmail: String): CustomerResponse {
        val seller = findSeller(sellerEmail)
        val buyer = userRepository.findById(customerId)
            .orElseThrow { EntityNotFoundException("Cliente", customerId) }

        val orderCount = orderRepository.countByBuyerAndSeller(buyer, seller)
        if (orderCount == 0L) {
            throw BusinessRuleViolationException("Este cliente no tiene órdenes con tu tienda")
        }

        return buildCustomerResponse(buyer, seller)
    }

    fun getCustomerStats(sellerEmail: String): CustomerStatsResponse {
        val seller = findSeller(sellerEmail)
        val now = LocalDateTime.now()
        val thirtyDaysAgo = now.minusDays(NEW_DAYS)

        val totalCustomers = orderRepository.countDistinctBuyersBySeller(seller)
        val activeCustomers = orderRepository.countDistinctBuyersBySellerSince(seller, thirtyDaysAgo)

        val buyers = orderRepository.findDistinctBuyersBySeller(seller)
        val customerResponses = buyers.map { buyer -> buildCustomerResponse(buyer, seller) }

        val newCustomers = customerResponses.count { c ->
            c.registeredAt.isAfter(thirtyDaysAgo)
        }.toLong()

        val vipCustomers = customerResponses.count { c ->
            c.segment == "VIP"
        }.toLong()

        val totalRevenue = customerResponses.fold(BigDecimal.ZERO) { acc, c ->
            acc.add(c.totalSpent)
        }

        val avgCustomerValue = if (totalCustomers > 0) {
            totalRevenue.divide(BigDecimal(totalCustomers), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return CustomerStatsResponse(
            totalCustomers = totalCustomers,
            activeCustomers = activeCustomers,
            newCustomers = newCustomers,
            vipCustomers = vipCustomers,
            totalRevenue = totalRevenue,
            avgCustomerValue = avgCustomerValue
        )
    }

    fun getCustomersForExport(
        sellerEmail: String,
        segment: String? = null
    ): List<CustomerResponse> {
        return getCustomersBySeller(sellerEmail, segment = segment)
    }

    private fun buildCustomerResponse(buyer: User, seller: User): CustomerResponse {
        val profile = userProfileRepository.findByUser(buyer).orElse(null)
        val totalOrders = orderRepository.countByBuyerAndSeller(buyer, seller)
        val totalSpent = orderRepository.sumTotalByBuyerAndSeller(buyer, seller)
        val lastOrderDate = orderRepository.findLastOrderDateByBuyerAndSeller(buyer, seller)

        val avgOrderValue = if (totalOrders > 0) {
            totalSpent.divide(BigDecimal(totalOrders), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val segment = calculateSegment(totalSpent, totalOrders, buyer.createdAt, lastOrderDate)

        return CustomerResponse(
            id = buyer.id!!,
            name = buyer.name,
            email = buyer.email,
            phone = profile?.phone,
            avatarUrl = profile?.avatarUrl,
            address = profile?.address,
            city = profile?.city,
            state = profile?.state,
            postalCode = profile?.postalCode,
            country = profile?.country ?: "Colombia",
            isActive = buyer.isActive ?: true,
            totalOrders = totalOrders,
            totalSpent = totalSpent,
            avgOrderValue = avgOrderValue,
            lastOrderDate = lastOrderDate,
            segment = segment,
            registeredAt = buyer.createdAt
        )
    }

    private fun calculateSegment(
        totalSpent: BigDecimal,
        totalOrders: Long,
        registeredAt: LocalDateTime,
        lastOrderDate: LocalDateTime?
    ): String {
        val now = LocalDateTime.now()

        if (totalSpent >= VIP_SPENT_THRESHOLD || totalOrders >= VIP_ORDERS_THRESHOLD) {
            return "VIP"
        }
        if (totalOrders >= FREQUENT_ORDERS_THRESHOLD) {
            return "FREQUENT"
        }
        if (registeredAt.isAfter(now.minusDays(NEW_DAYS))) {
            return "NEW"
        }
        if (lastOrderDate == null || lastOrderDate.isBefore(now.minusDays(INACTIVE_DAYS))) {
            return "INACTIVE"
        }
        return "REGULAR"
    }

    private fun findSeller(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Vendedor", email) }
    }
}
