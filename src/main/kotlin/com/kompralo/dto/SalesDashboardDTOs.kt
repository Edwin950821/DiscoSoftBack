package com.kompralo.dto

import java.math.BigDecimal

/**
 * DTO para métricas de ventas en un período
 */
data class SalesPeriodMetrics(
    val count: Long,
    val total: BigDecimal,
    val percentageChange: BigDecimal? = null
)

/**
 * DTO para conteo de órdenes por estado
 */
data class OrderStatusCount(
    val pending: Long,
    val processing: Long,
    val shipped: Long,
    val delivered: Long
)

/**
 * DTO de respuesta para dashboard de ventas del vendedor
 */
data class SalesDashboardResponse(

    val today: SalesPeriodMetrics,

    val thisWeek: SalesPeriodMetrics,

    val thisMonth: SalesPeriodMetrics,

    val averageTicket: BigDecimal,

    val ordersByStatus: OrderStatusCount,

    val totalOrders: Long,
    val totalRevenue: BigDecimal
)
