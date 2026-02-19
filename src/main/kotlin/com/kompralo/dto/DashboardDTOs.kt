package com.kompralo.dto

import java.math.BigDecimal

data class DashboardMetricsResponse(
    val totalRevenue: BigDecimal,
    val revenueChange: Double,
    val totalOrders: Long,
    val ordersChange: Double,
    val averageTicket: BigDecimal,
    val ticketChange: Double,
    val conversionRate: Double,
    val conversionChange: Double,
    val totalCustomers: Long,
    val newCustomers: Long,
)

data class DailyIncomeResponse(
    val day: String,
    val date: String,
    val actual: BigDecimal,
    val anterior: BigDecimal,
    val orders: Long,
)

data class CategorySalesResponse(
    val category: String,
    val ventas: BigDecimal,
    val productCount: Int,
)

data class StockSummaryResponse(
    val inStock: Int,
    val lowStock: Int,
    val outOfStock: Int,
    val totalProducts: Int,
)

data class RegionSalesResponse(
    val city: String,
    val ventas: BigDecimal,
    val orders: Long,
)

data class CustomerSummaryResponse(
    val totalCustomers: Long,
    val newCustomersPct: Double,
    val returningCustomersPct: Double,
    val funnelVisits: Long,
    val funnelCart: Long,
    val funnelPurchase: Long,
)
