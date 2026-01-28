package com.kompralo.dto

import java.math.BigDecimal

/**
 * DTO para métricas de ventas en un período
 */
data class SalesPeriodMetrics(
    val count: Long,          // Cantidad de órdenes
    val total: BigDecimal,    // Total vendido
    val percentageChange: BigDecimal? = null // Cambio porcentual vs período anterior
)

/**
 * DTO para conteo de órdenes por estado
 */
data class OrderStatusCount(
    val pending: Long,      // PENDING
    val processing: Long,   // CONFIRMED + PROCESSING
    val shipped: Long,      // SHIPPED (en tránsito)
    val delivered: Long     // DELIVERED
)

/**
 * DTO de respuesta para dashboard de ventas del vendedor
 */
data class SalesDashboardResponse(
    // Métricas de hoy
    val today: SalesPeriodMetrics,

    // Métricas de la semana
    val thisWeek: SalesPeriodMetrics,

    // Métricas del mes
    val thisMonth: SalesPeriodMetrics,

    // Ticket promedio del mes
    val averageTicket: BigDecimal,

    // Conteo de órdenes por estado
    val ordersByStatus: OrderStatusCount,

    // Información adicional
    val totalOrders: Long,          // Total de órdenes históricas
    val totalRevenue: BigDecimal    // Ingreso total histórico
)
