package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.OrderStatus
import com.kompralo.model.ProductStatus
import com.kompralo.model.User
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.ProductRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class FinanceService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) {

    private val EXPENSE_RATIO = BigDecimal("0.65")
    private val IVA_RATE = BigDecimal("0.19")
    private val COGS_RATIO = BigDecimal("0.55")
    private val SHIPPING_RATIO = BigDecimal("0.08")
    private val OPERATING_RATIO = BigDecimal("0.05")

    private fun findSeller(email: String): User =
        userRepository.findByEmail(email).orElseThrow { RuntimeException("Usuario no encontrado") }

    private fun monthRange(ym: YearMonth): Pair<LocalDateTime, LocalDateTime> =
        ym.atDay(1).atStartOfDay() to ym.atEndOfMonth().atTime(23, 59, 59)

    private fun revenueForMonth(seller: User, ym: YearMonth): BigDecimal {
        val (start, end) = monthRange(ym)
        return orderRepository.sumTotalBySellerAndDateRange(seller, start, end)
    }

    private fun ordersForMonth(seller: User, ym: YearMonth): Long {
        val (start, end) = monthRange(ym)
        return orderRepository.countOrdersBySellerAndDateRange(seller, start, end)
    }

    private fun pctChange(current: BigDecimal, previous: BigDecimal): Double {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return if (current > BigDecimal.ZERO) 100.0 else 0.0
        return current.subtract(previous).multiply(BigDecimal(100))
            .divide(previous, 2, RoundingMode.HALF_UP).toDouble()
    }

    private fun spanishMonth(ym: YearMonth): String =
        ym.month.getDisplayName(TextStyle.FULL, Locale("es", "CO"))
            .replaceFirstChar { it.uppercase() }

    private fun shortMonth(ym: YearMonth): String =
        ym.month.getDisplayName(TextStyle.SHORT, Locale("es", "CO"))
            .replaceFirstChar { it.uppercase() }

    // ── /finance/summary ──
    fun getSummary(email: String): FinanceSummaryResponse {
        val seller = findSeller(email)
        val now = YearMonth.now()
        val prev = now.minusMonths(1)
        val income = revenueForMonth(seller, now)
        val prevIncome = revenueForMonth(seller, prev)
        val expenses = income.multiply(EXPENSE_RATIO).setScale(0, RoundingMode.HALF_UP)
        val balance = income.subtract(expenses)
        val pendingOrders = orderRepository.countBySellerAndStatus(seller, OrderStatus.CONFIRMED) +
                orderRepository.countBySellerAndStatus(seller, OrderStatus.PROCESSING)

        return FinanceSummaryResponse(
            availableBalance = balance,
            balanceChange = pctChange(income, prevIncome),
            totalIncome = income,
            totalExpenses = expenses,
            accountsReceivable = income.multiply(BigDecimal("0.15")).setScale(0, RoundingMode.HALF_UP),
            receivableInvoices = pendingOrders,
            accountsPayable = expenses.multiply(BigDecimal("0.12")).setScale(0, RoundingMode.HALF_UP),
            payableNextDue = "15 Dias",
        )
    }

    // ── /finance/income-expense ──
    fun getIncomeExpense(email: String): List<IncomeExpensePointResponse> {
        val seller = findSeller(email)
        val now = YearMonth.now()
        return (5 downTo 0).map { offset ->
            val ym = now.minusMonths(offset.toLong())
            val income = revenueForMonth(seller, ym)
            val expenses = income.multiply(EXPENSE_RATIO).setScale(0, RoundingMode.HALF_UP)
            IncomeExpensePointResponse(
                month = shortMonth(ym),
                income = income,
                expenses = expenses,
            )
        }
    }

    // ── /finance/projection ──
    fun getProjection(email: String): List<BalanceProjectionResponse> {
        val seller = findSeller(email)
        val now = YearMonth.now()
        val last3 = (1..3).map { revenueForMonth(seller, now.minusMonths(it.toLong())) }
        val avg = if (last3.isNotEmpty()) last3.reduce(BigDecimal::add).divide(BigDecimal(last3.size), 0, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val growthRate = BigDecimal("1.05")
        return (1..3).map { offset ->
            val ym = now.plusMonths(offset.toLong())
            val projected = avg.multiply(growthRate.pow(offset)).setScale(0, RoundingMode.HALF_UP)
            BalanceProjectionResponse(
                month = shortMonth(ym).uppercase(),
                amount = projected,
            )
        }
    }

    // ── /finance/monthly ──
    fun getMonthlyHistory(email: String): List<MonthlyRecordResponse> {
        val seller = findSeller(email)
        val now = YearMonth.now()
        return (0..3).map { offset ->
            val ym = now.minusMonths(offset.toLong())
            val income = revenueForMonth(seller, ym)
            val expenses = income.multiply(EXPENSE_RATIO).setScale(0, RoundingMode.HALF_UP)
            MonthlyRecordResponse(
                month = spanishMonth(ym),
                isCurrent = offset == 0,
                income = income,
                balance = income.subtract(expenses),
            )
        }
    }

    // ── /finance/costs ──
    fun getCostBreakdown(email: String): CostBreakdownResponse {
        val seller = findSeller(email)
        val now = YearMonth.now()
        val prev = now.minusMonths(1)
        val income = revenueForMonth(seller, now)
        val prevIncome = revenueForMonth(seller, prev)

        val cogs = income.multiply(COGS_RATIO).setScale(0, RoundingMode.HALF_UP)
        val prevCogs = prevIncome.multiply(COGS_RATIO).setScale(0, RoundingMode.HALF_UP)
        val shipping = income.multiply(SHIPPING_RATIO).setScale(0, RoundingMode.HALF_UP)
        val prevShipping = prevIncome.multiply(SHIPPING_RATIO).setScale(0, RoundingMode.HALF_UP)
        val operating = income.multiply(OPERATING_RATIO).setScale(0, RoundingMode.HALF_UP)
        val prevOperating = prevIncome.multiply(OPERATING_RATIO).setScale(0, RoundingMode.HALF_UP)
        val totalExpenses = cogs.add(shipping).add(operating)
        val margin = if (income > BigDecimal.ZERO)
            income.subtract(totalExpenses).multiply(BigDecimal(100)).divide(income, 1, RoundingMode.HALF_UP).toDouble()
        else 0.0
        val breakEven = totalExpenses
        val status = when {
            margin >= 25 -> "healthy"
            margin >= 15 -> "moderate"
            else -> "risk"
        }

        return CostBreakdownResponse(
            cogs = cogs, cogsChange = pctChange(cogs, prevCogs),
            shippingCosts = shipping, shippingChange = pctChange(shipping, prevShipping),
            operatingCosts = operating, operatingChange = pctChange(operating, prevOperating),
            profitMargin = margin, breakEvenAmount = breakEven, breakEvenStatus = status,
        )
    }

    // ── /finance/category-profit ──
    fun getCategoryProfitability(email: String): List<CategoryProfitabilityResponse> {
        val seller = findSeller(email)
        val products = productRepository.findBySellerId(seller.id!!)
        return products.groupBy { it.category }.map { (category, prods) ->
            val revenue = prods.sumOf { it.price.multiply(BigDecimal(it.sales)) }
            val profit = revenue.multiply(BigDecimal("0.30")).setScale(0, RoundingMode.HALF_UP)
            CategoryProfitabilityResponse(category = category, revenue = revenue, netProfit = profit)
        }.sortedByDescending { it.revenue }
    }

    // ── /finance/inventory ──
    fun getInventoryFinance(email: String): InventoryFinanceResponse {
        val seller = findSeller(email)
        val products = productRepository.findBySellerId(seller.id!!)
        val totalValue = products.sumOf { it.price.multiply(BigDecimal(it.stock)) }
        val totalSales = products.sumOf { it.sales }
        val totalStock = products.sumOf { it.stock }
        val rotation = if (totalStock > 0) totalSales.toDouble() / totalStock else 0.0
        val daysStock = if (rotation > 0) (365.0 / rotation).toInt().coerceAtMost(999) else 0

        return InventoryFinanceResponse(
            totalValue = totalValue,
            valueChange = 0.0,
            rotationRate = (rotation * 10).toInt() / 10.0,
            daysStock = daysStock,
        )
    }

    // ── /finance/slow-moving ──
    fun getSlowMovingProducts(email: String): List<SlowMovingProductResponse> {
        val seller = findSeller(email)
        val products = productRepository.findBySellerIdAndStatus(seller.id!!, ProductStatus.ACTIVE)
        val now = LocalDateTime.now()
        return products
            .sortedBy { it.sales }
            .take(5)
            .map {
                val daysSince = ChronoUnit.DAYS.between(it.updatedAt, now).coerceAtLeast(1)
                SlowMovingProductResponse(name = it.name, sku = it.sku, imageUrl = it.imageUrl, daysSinceLastSale = daysSince)
            }
    }

    // ── /finance/stock-projection ──
    fun getStockProjection(email: String): List<StockProjectionPointResponse> {
        val seller = findSeller(email)
        val products = productRepository.findBySellerIdAndStatus(seller.id!!, ProductStatus.ACTIVE)
        return products.take(5).map { p ->
            val dailyRate = if (p.sales > 0) (p.sales.toDouble() / 30).coerceAtLeast(0.1) else 0.1
            val projected = (p.stock - (dailyRate * 30)).toInt().coerceAtLeast(0)
            StockProjectionPointResponse(label = p.sku, current = p.stock, projected = projected)
        }
    }

    // ── /finance/customer-roi ──
    fun getCustomerROI(email: String): CustomerROIResponse {
        val seller = findSeller(email)
        val now = YearMonth.now()
        val (start, end) = monthRange(now)
        val totalRevenue = orderRepository.sumTotalBySellerAndDateRange(seller, start, end)
        val totalCustomers = orderRepository.countDistinctBuyersBySellerSince(seller, start).coerceAtLeast(1)
        val clv = totalRevenue.divide(BigDecimal(totalCustomers), 0, RoundingMode.HALF_UP)
        val cac = clv.multiply(BigDecimal("0.25")).setScale(0, RoundingMode.HALF_UP)
        val roi = if (cac > BigDecimal.ZERO) clv.divide(cac, 1, RoundingMode.HALF_UP).toDouble() else 0.0

        return CustomerROIResponse(
            averageCLV = clv, clvChange = 0.0,
            averageCAC = cac, cacChange = 0.0,
            roiTotal = roi, roiTarget = 4.0,
        )
    }

    // ── /finance/segments ──
    fun getFinancialSegments(email: String): List<FinancialSegmentResponse> {
        return listOf(
            FinancialSegmentResponse("VIP", 62.0, "Revenue", 8.2, 0, "up", "#059669", "#064e3b"),
            FinancialSegmentResponse("Frecuentes", 28.0, "Revenue", 2.5, 0, "stable", "#6b7280", "#374151"),
            FinancialSegmentResponse("Riesgo de Churn", 10.0, "Revenue", 0.8, 0, "down", "#ef4444", "#7f1d1d"),
        )
    }

    // ── /finance/tax-summary ──
    fun getTaxSummary(email: String): TaxSummaryResponse {
        val seller = findSeller(email)
        val now = YearMonth.now()
        val income = revenueForMonth(seller, now)
        val ivaCollected = income.multiply(IVA_RATE).setScale(0, RoundingMode.HALF_UP)
        val ivaPending = ivaCollected.multiply(BigDecimal("0.25")).setScale(0, RoundingMode.HALF_UP)

        return TaxSummaryResponse(
            ivaCollected = ivaCollected,
            ivaPending = ivaPending,
            status = "current",
            creditProjection = ivaCollected.multiply(BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP),
        )
    }

    // ── /finance/retentions ──
    fun getTaxRetentions(email: String): List<TaxRetentionResponse> {
        findSeller(email)
        return emptyList()
    }

    // ── /finance/tax-trend ──
    fun getTaxTrend(email: String): List<TaxTrendPointResponse> {
        val seller = findSeller(email)
        val now = YearMonth.now()
        return (5 downTo 0).map { offset ->
            val ym = now.minusMonths(offset.toLong())
            val income = revenueForMonth(seller, ym)
            val collected = income.multiply(IVA_RATE).setScale(0, RoundingMode.HALF_UP)
            val paid = collected.multiply(BigDecimal("0.80")).setScale(0, RoundingMode.HALF_UP)
            TaxTrendPointResponse(month = shortMonth(ym), collected = collected, paid = paid)
        }
    }

    // ── /finance/channels ──
    fun getChannelData(email: String): List<ChannelDataResponse> {
        return listOf(
            ChannelDataResponse("Online Store", 100.0, "#3b82f6"),
        )
    }

    // ── /finance/regions ──
    fun getRegionalPerformance(email: String): List<RegionalPerformanceResponse> {
        val seller = findSeller(email)
        val now = YearMonth.now()
        val (start, end) = monthRange(now)
        val orders = orderRepository.findBySellerAndCreatedAtBetween(seller, start, end)
        val byCity = orders.filter { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.REFUNDED }
            .groupBy { it.shippingCity }
        return byCity.map { (city, cityOrders) ->
            val revenue = cityOrders.sumOf { it.total }
            RegionalPerformanceResponse(region = city, subtitle = "", revenue = revenue, yoyChange = 0.0)
        }.sortedByDescending { it.revenue }.take(5)
    }

    // ── /finance/channel-summary ──
    fun getChannelSummary(email: String): ChannelSummaryResponse {
        val seller = findSeller(email)
        val now = YearMonth.now()
        val prev = now.minusMonths(1)
        val revenue = revenueForMonth(seller, now)
        val prevRevenue = revenueForMonth(seller, prev)
        val orders = ordersForMonth(seller, now)
        val prevOrders = ordersForMonth(seller, prev)
        val avg = if (orders > 0) revenue.divide(BigDecimal(orders), 0, RoundingMode.HALF_UP) else BigDecimal.ZERO
        val prevAvg = if (prevOrders > 0) prevRevenue.divide(BigDecimal(prevOrders), 0, RoundingMode.HALF_UP) else BigDecimal.ZERO

        return ChannelSummaryResponse(
            totalRevenue = revenue,
            revenueChange = pctChange(revenue, prevRevenue),
            avgOrderValue = avg,
            avgOrderChange = pctChange(avg, prevAvg),
        )
    }
}
