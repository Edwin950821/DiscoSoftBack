package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.OrderStatus
import com.kompralo.model.ProductStatus
import com.kompralo.repository.AnalyticsEventRepository
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.*

@Service
class DashboardService(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val analyticsEventRepository: AnalyticsEventRepository,
) {

    private fun findSellerByEmail(email: String) =
        userRepository.findByEmail(email).orElseThrow { RuntimeException("Usuario no encontrado") }

    private fun pctChange(current: BigDecimal, previous: BigDecimal): Double {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return if (current.compareTo(BigDecimal.ZERO) > 0) 100.0 else 0.0
        }
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    private fun pctChangeLong(current: Long, previous: Long): Double {
        if (previous == 0L) return if (current > 0) 100.0 else 0.0
        return ((current - previous).toDouble() / previous.toDouble() * 100.0)
            .toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    fun getMetrics(email: String): DashboardMetricsResponse {
        val seller = findSellerByEmail(email)
        val now = LocalDateTime.now()

        val startOfWeek = now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay()
        val startOfPrevWeek = startOfWeek.minusWeeks(1)

        val curRevenue = orderRepository.sumTotalBySellerAndDateRange(seller, startOfWeek, now)
        val curOrders = orderRepository.countOrdersBySellerAndDateRange(seller, startOfWeek, now)
        val curTicket = orderRepository.averageTicketBySellerAndDateRange(seller, startOfWeek, now)

        val prevRevenue = orderRepository.sumTotalBySellerAndDateRange(seller, startOfPrevWeek, startOfWeek)
        val prevOrders = orderRepository.countOrdersBySellerAndDateRange(seller, startOfPrevWeek, startOfWeek)
        val prevTicket = orderRepository.averageTicketBySellerAndDateRange(seller, startOfPrevWeek, startOfWeek)

        val totalCustomers = orderRepository.countDistinctBuyersBySeller(seller)
        val newCustomers = orderRepository.countDistinctBuyersBySellerSince(seller, startOfWeek)

        val completedOrders = orderRepository.findBySellerAndCreatedAtBetween(seller, startOfWeek, now)
            .count { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.REFUNDED }
        val totalOrdersInPeriod = curOrders
        val conversionRate = if (totalOrdersInPeriod > 0)
            (completedOrders.toDouble() / totalOrdersInPeriod.toDouble() * 100.0)
                .toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
        else 0.0

        return DashboardMetricsResponse(
            totalRevenue = curRevenue,
            revenueChange = pctChange(curRevenue, prevRevenue),
            totalOrders = curOrders,
            ordersChange = pctChangeLong(curOrders, prevOrders),
            averageTicket = curTicket,
            ticketChange = pctChange(curTicket, prevTicket),
            conversionRate = conversionRate,
            conversionChange = 0.0,
            totalCustomers = totalCustomers,
            newCustomers = newCustomers,
        )
    }

    fun getDailyIncome(email: String, days: Int = 7): List<DailyIncomeResponse> {
        val seller = findSellerByEmail(email)
        val today = LocalDate.now()

        val startCurrent = today.minusDays(days.toLong() - 1).atStartOfDay()
        val endCurrent = today.plusDays(1).atStartOfDay()

        val startPrevious = startCurrent.minusDays(days.toLong())

        val currentOrders = orderRepository.findBySellerAndCreatedAtBetween(seller, startCurrent, endCurrent)
            .filter { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.REFUNDED }
        val previousOrders = orderRepository.findBySellerAndCreatedAtBetween(seller, startPrevious, startCurrent)
            .filter { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.REFUNDED }

        val currentByDay = currentOrders.groupBy { it.createdAt.toLocalDate() }
        val previousByDay = previousOrders.groupBy { it.createdAt.toLocalDate() }

        val dayNames = mapOf(
            DayOfWeek.MONDAY to "Lun", DayOfWeek.TUESDAY to "Mar", DayOfWeek.WEDNESDAY to "Mie",
            DayOfWeek.THURSDAY to "Jue", DayOfWeek.FRIDAY to "Vie", DayOfWeek.SATURDAY to "Sab",
            DayOfWeek.SUNDAY to "Dom"
        )

        return (0 until days).map { offset ->
            val date = today.minusDays((days - 1 - offset).toLong())
            val prevDate = date.minusDays(days.toLong())

            val dayOrders = currentByDay[date] ?: emptyList()
            val prevDayOrders = previousByDay[prevDate] ?: emptyList()

            val actualIncome = dayOrders.sumOf { it.total }
            val anteriorIncome = prevDayOrders.sumOf { it.total }

            DailyIncomeResponse(
                day = dayNames[date.dayOfWeek] ?: date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")),
                date = date.toString(),
                actual = actualIncome,
                anterior = anteriorIncome,
                orders = dayOrders.size.toLong(),
            )
        }
    }

    fun getTopCategories(email: String, limit: Int = 5): List<CategorySalesResponse> {
        val seller = findSellerByEmail(email)
        val products = productRepository.findBySellerId(seller.id!!)

        return products
            .groupBy { it.category }
            .map { (category, prods) ->
                val totalSales = prods.sumOf { it.price.multiply(BigDecimal(it.sales)) }
                CategorySalesResponse(
                    category = category,
                    ventas = totalSales,
                    productCount = prods.size,
                )
            }
            .sortedByDescending { it.ventas }
            .take(limit)
    }

    fun getStockSummary(email: String): StockSummaryResponse {
        val seller = findSellerByEmail(email)
        val products = productRepository.findBySellerId(seller.id!!)

        val inStock = products.count { it.status == ProductStatus.ACTIVE && it.stock > 10 }
        val lowStock = products.count { it.status == ProductStatus.ACTIVE && it.stock in 1..10 }
        val outOfStock = products.count { it.status == ProductStatus.OUT_OF_STOCK || it.stock == 0 }

        return StockSummaryResponse(
            inStock = inStock,
            lowStock = lowStock,
            outOfStock = outOfStock,
            totalProducts = products.size,
        )
    }

    fun getRegionSales(email: String, limit: Int = 5): List<RegionSalesResponse> {
        val seller = findSellerByEmail(email)

        val since = LocalDateTime.now().minusDays(30)
        val orders = orderRepository.findBySellerAndCreatedAtBetween(seller, since, LocalDateTime.now())
            .filter { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.REFUNDED }

        return orders
            .groupBy { it.shippingCity.trim().lowercase().replaceFirstChar { c -> c.uppercase() } }
            .map { (city, cityOrders) ->
                RegionSalesResponse(
                    city = city,
                    ventas = cityOrders.sumOf { it.total },
                    orders = cityOrders.size.toLong(),
                )
            }
            .sortedByDescending { it.ventas }
            .take(limit)
    }

    fun getCustomerSummary(email: String): CustomerSummaryResponse {
        val seller = findSellerByEmail(email)

        val totalCustomers = orderRepository.countDistinctBuyersBySeller(seller)
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val newCustomers = orderRepository.countDistinctBuyersBySellerSince(seller, thirtyDaysAgo)
        val returningCustomers = if (totalCustomers > newCustomers) totalCustomers - newCustomers else 0

        val newPct = if (totalCustomers > 0) (newCustomers.toDouble() / totalCustomers.toDouble() * 100.0)
            .toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble() else 0.0
        val retPct = if (totalCustomers > 0) (returningCustomers.toDouble() / totalCustomers.toDouble() * 100.0)
            .toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble() else 0.0

        val funnelVisits = analyticsEventRepository.countDistinctSessionsBySellerAndType(
            seller.id!!, "PAGE_VIEW", thirtyDaysAgo
        )
        val funnelCart = analyticsEventRepository.countDistinctSessionsBySellerAndType(
            seller.id!!, "ADD_TO_CART", thirtyDaysAgo
        )
        val funnelPurchase = orderRepository.countOrdersBySellerAndDateRange(
            seller, thirtyDaysAgo, LocalDateTime.now()
        )

        return CustomerSummaryResponse(
            totalCustomers = totalCustomers,
            newCustomersPct = newPct,
            returningCustomersPct = retPct,
            funnelVisits = funnelVisits,
            funnelCart = funnelCart,
            funnelPurchase = funnelPurchase,
        )
    }
}
