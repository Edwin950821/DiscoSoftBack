package com.kompralo.dto

import java.math.BigDecimal

data class SalesPeriodMetrics(
    val count: Long,
    val total: BigDecimal,
    val percentageChange: BigDecimal? = null
)

data class OrderStatusCount(
    val pending: Long,
    val processing: Long,
    val shipped: Long,
    val delivered: Long
)

data class SalesDashboardResponse(

    val today: SalesPeriodMetrics,

    val thisWeek: SalesPeriodMetrics,

    val thisMonth: SalesPeriodMetrics,

    val averageTicket: BigDecimal,

    val ordersByStatus: OrderStatusCount,

    val totalOrders: Long,
    val totalRevenue: BigDecimal
)
