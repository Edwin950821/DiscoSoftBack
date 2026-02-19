package com.kompralo.dto

import java.math.BigDecimal

data class FinanceSummaryResponse(
    val availableBalance: BigDecimal,
    val balanceChange: Double,
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val accountsReceivable: BigDecimal,
    val receivableInvoices: Long,
    val accountsPayable: BigDecimal,
    val payableNextDue: String,
)

data class IncomeExpensePointResponse(
    val month: String,
    val income: BigDecimal,
    val expenses: BigDecimal,
)

data class BalanceProjectionResponse(
    val month: String,
    val amount: BigDecimal,
)

data class MonthlyRecordResponse(
    val month: String,
    val isCurrent: Boolean = false,
    val income: BigDecimal,
    val balance: BigDecimal,
)

data class CostBreakdownResponse(
    val cogs: BigDecimal,
    val cogsChange: Double,
    val shippingCosts: BigDecimal,
    val shippingChange: Double,
    val operatingCosts: BigDecimal,
    val operatingChange: Double,
    val profitMargin: Double,
    val breakEvenAmount: BigDecimal,
    val breakEvenStatus: String,
)

data class CategoryProfitabilityResponse(
    val category: String,
    val revenue: BigDecimal,
    val netProfit: BigDecimal,
)

data class InventoryFinanceResponse(
    val totalValue: BigDecimal,
    val valueChange: Double,
    val rotationRate: Double,
    val daysStock: Int,
)

data class SlowMovingProductResponse(
    val name: String,
    val sku: String,
    val imageUrl: String?,
    val daysSinceLastSale: Long,
)

data class StockProjectionPointResponse(
    val label: String,
    val current: Int,
    val projected: Int,
)

data class CustomerROIResponse(
    val averageCLV: BigDecimal,
    val clvChange: Double,
    val averageCAC: BigDecimal,
    val cacChange: Double,
    val roiTotal: Double,
    val roiTarget: Double,
)

data class FinancialSegmentResponse(
    val name: String,
    val contributionPct: Double,
    val contributionLabel: String,
    val roi: Double,
    val customers: Long,
    val trend: String,
    val color: String,
    val iconBg: String,
)

data class TaxSummaryResponse(
    val ivaCollected: BigDecimal,
    val ivaPending: BigDecimal,
    val status: String,
    val creditProjection: BigDecimal,
)

data class TaxRetentionResponse(
    val id: Long,
    val name: String,
    val date: String,
    val reference: String,
    val amount: BigDecimal,
    val type: String,
    val status: String,
)

data class TaxTrendPointResponse(
    val month: String,
    val collected: BigDecimal,
    val paid: BigDecimal,
)

data class ChannelDataResponse(
    val name: String,
    val percentage: Double,
    val color: String,
)

data class RegionalPerformanceResponse(
    val region: String,
    val subtitle: String,
    val revenue: BigDecimal,
    val yoyChange: Double,
)

data class ChannelSummaryResponse(
    val totalRevenue: BigDecimal,
    val revenueChange: Double,
    val avgOrderValue: BigDecimal,
    val avgOrderChange: Double,
)
