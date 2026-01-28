package com.kompralo.services

import com.kompralo.dto.OrderStatusCount
import com.kompralo.dto.SalesDashboardResponse
import com.kompralo.dto.SalesPeriodMetrics
import com.kompralo.model.OrderStatus
import com.kompralo.model.User
import com.kompralo.repository.OrderRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Servicio para dashboard de ventas del vendedor
 */
@Service
class SalesDashboardService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) {

    /**
     * Obtiene las métricas del dashboard de ventas para un vendedor
     */
    @Transactional(readOnly = true)
    fun getSalesDashboard(email: String): SalesDashboardResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        // Calcular métricas de hoy
        val todayMetrics = calculatePeriodMetrics(seller, PeriodType.TODAY)

        // Calcular métricas de la semana
        val weekMetrics = calculatePeriodMetrics(seller, PeriodType.WEEK)

        // Calcular métricas del mes
        val monthMetrics = calculatePeriodMetrics(seller, PeriodType.MONTH)

        // Calcular ticket promedio del mes
        val averageTicket = calculateAverageTicket(seller, PeriodType.MONTH)

        // Contar órdenes por estado
        val ordersByStatus = countOrdersByStatus(seller)

        // Calcular totales históricos (solo órdenes PAGADAS)
        val totalOrders = orderRepository.findBySellerOrderByCreatedAtDesc(seller).size.toLong()
        val totalRevenue = orderRepository.findBySellerOrderByCreatedAtDesc(seller)
            .filter {
                it.paymentStatus == "PAID" &&
                it.status != OrderStatus.CANCELLED &&
                it.status != OrderStatus.REFUNDED
            }
            .sumOf { it.total }

        return SalesDashboardResponse(
            today = todayMetrics,
            thisWeek = weekMetrics,
            thisMonth = monthMetrics,
            averageTicket = averageTicket,
            ordersByStatus = ordersByStatus,
            totalOrders = totalOrders,
            totalRevenue = totalRevenue
        )
    }

    /**
     * Calcula métricas para un período específico
     */
    private fun calculatePeriodMetrics(seller: User, periodType: PeriodType): SalesPeriodMetrics {
        val (startDate, endDate) = getPeriodDates(periodType)
        val (prevStartDate, prevEndDate) = getPreviousPeriodDates(periodType)

        // Métricas del período actual
        val count = orderRepository.countOrdersBySellerAndDateRange(seller, startDate, endDate)
        val total = orderRepository.sumTotalBySellerAndDateRange(seller, startDate, endDate)

        // Métricas del período anterior
        val prevTotal = orderRepository.sumTotalBySellerAndDateRange(seller, prevStartDate, prevEndDate)

        // Calcular porcentaje de cambio
        val percentageChange = if (prevTotal > BigDecimal.ZERO) {
            ((total - prevTotal) / prevTotal * BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else if (total > BigDecimal.ZERO) {
            BigDecimal(100) // Si no había ventas antes y ahora sí, es 100% de incremento
        } else {
            BigDecimal.ZERO
        }

        return SalesPeriodMetrics(
            count = count,
            total = total,
            percentageChange = percentageChange
        )
    }

    /**
     * Calcula el ticket promedio para un período
     */
    private fun calculateAverageTicket(seller: User, periodType: PeriodType): BigDecimal {
        val (startDate, endDate) = getPeriodDates(periodType)
        return orderRepository.averageTicketBySellerAndDateRange(seller, startDate, endDate)
            .setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Cuenta las órdenes del vendedor por estado
     */
    private fun countOrdersByStatus(seller: User): OrderStatusCount {
        return OrderStatusCount(
            pending = orderRepository.countBySellerAndStatus(seller, OrderStatus.PENDING),
            processing = orderRepository.countBySellerAndStatus(seller, OrderStatus.CONFIRMED) +
                        orderRepository.countBySellerAndStatus(seller, OrderStatus.PROCESSING),
            shipped = orderRepository.countBySellerAndStatus(seller, OrderStatus.SHIPPED),
            delivered = orderRepository.countBySellerAndStatus(seller, OrderStatus.DELIVERED)
        )
    }

    /**
     * Obtiene las fechas de inicio y fin para un período
     */
    private fun getPeriodDates(periodType: PeriodType): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDateTime.now()

        return when (periodType) {
            PeriodType.TODAY -> {
                val startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
                val endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                Pair(startOfDay, endOfDay)
            }

            PeriodType.WEEK -> {
                // Semana actual (lunes a domingo)
                val today = LocalDate.now()
                val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                val sunday = monday.plusDays(6)

                val startOfWeek = LocalDateTime.of(monday, LocalTime.MIN)
                val endOfWeek = LocalDateTime.of(sunday, LocalTime.MAX)
                Pair(startOfWeek, endOfWeek)
            }

            PeriodType.MONTH -> {
                // Mes actual
                val firstDayOfMonth = LocalDate.now().withDayOfMonth(1)
                val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

                val startOfMonth = LocalDateTime.of(firstDayOfMonth, LocalTime.MIN)
                val endOfMonth = LocalDateTime.of(lastDayOfMonth, LocalTime.MAX)
                Pair(startOfMonth, endOfMonth)
            }
        }
    }

    /**
     * Obtiene las fechas del período anterior (para comparación)
     */
    private fun getPreviousPeriodDates(periodType: PeriodType): Pair<LocalDateTime, LocalDateTime> {
        return when (periodType) {
            PeriodType.TODAY -> {
                // Ayer
                val yesterday = LocalDate.now().minusDays(1)
                val startOfDay = LocalDateTime.of(yesterday, LocalTime.MIN)
                val endOfDay = LocalDateTime.of(yesterday, LocalTime.MAX)
                Pair(startOfDay, endOfDay)
            }

            PeriodType.WEEK -> {
                // Semana pasada
                val today = LocalDate.now()
                val lastMonday = today.minusDays(today.dayOfWeek.value.toLong() + 6)
                val lastSunday = lastMonday.plusDays(6)

                val startOfWeek = LocalDateTime.of(lastMonday, LocalTime.MIN)
                val endOfWeek = LocalDateTime.of(lastSunday, LocalTime.MAX)
                Pair(startOfWeek, endOfWeek)
            }

            PeriodType.MONTH -> {
                // Mes pasado
                val firstDayOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1)
                val lastDayOfLastMonth = firstDayOfLastMonth.plusMonths(1).minusDays(1)

                val startOfMonth = LocalDateTime.of(firstDayOfLastMonth, LocalTime.MIN)
                val endOfMonth = LocalDateTime.of(lastDayOfLastMonth, LocalTime.MAX)
                Pair(startOfMonth, endOfMonth)
            }
        }
    }

    /**
     * Enum para tipos de período
     */
    private enum class PeriodType {
        TODAY,
        WEEK,
        MONTH
    }
}
