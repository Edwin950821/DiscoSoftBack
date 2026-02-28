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

@Service
class SalesDashboardService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getSalesDashboard(email: String): SalesDashboardResponse {
        val seller = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val todayMetrics = calculatePeriodMetrics(seller, PeriodType.TODAY)
        val weekMetrics = calculatePeriodMetrics(seller, PeriodType.WEEK)
        val monthMetrics = calculatePeriodMetrics(seller, PeriodType.MONTH)
        val averageTicket = calculateAverageTicket(seller, PeriodType.MONTH)
        val ordersByStatus = countOrdersByStatus(seller)

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

    private fun calculatePeriodMetrics(seller: User, periodType: PeriodType): SalesPeriodMetrics {
        val (startDate, endDate) = getPeriodDates(periodType)
        val (prevStartDate, prevEndDate) = getPreviousPeriodDates(periodType)

        val count = orderRepository.countOrdersBySellerAndDateRange(seller, startDate, endDate)
        val total = orderRepository.sumTotalBySellerAndDateRange(seller, startDate, endDate)
        val prevTotal = orderRepository.sumTotalBySellerAndDateRange(seller, prevStartDate, prevEndDate)

        val percentageChange = if (prevTotal > BigDecimal.ZERO) {
            ((total - prevTotal) / prevTotal * BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
        } else if (total > BigDecimal.ZERO) {
            BigDecimal(100)
        } else {
            BigDecimal.ZERO
        }

        return SalesPeriodMetrics(
            count = count,
            total = total,
            percentageChange = percentageChange
        )
    }

    private fun calculateAverageTicket(seller: User, periodType: PeriodType): BigDecimal {
        val (startDate, endDate) = getPeriodDates(periodType)
        return orderRepository.averageTicketBySellerAndDateRange(seller, startDate, endDate)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun countOrdersByStatus(seller: User): OrderStatusCount {
        return OrderStatusCount(
            pending = orderRepository.countBySellerAndStatus(seller, OrderStatus.PENDING),
            processing = orderRepository.countBySellerAndStatus(seller, OrderStatus.CONFIRMED) +
                        orderRepository.countBySellerAndStatus(seller, OrderStatus.PROCESSING),
            shipped = orderRepository.countBySellerAndStatus(seller, OrderStatus.SHIPPED),
            delivered = orderRepository.countBySellerAndStatus(seller, OrderStatus.DELIVERED)
        )
    }

    private fun getPeriodDates(periodType: PeriodType): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDateTime.now()

        return when (periodType) {
            PeriodType.TODAY -> {
                val startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
                val endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                Pair(startOfDay, endOfDay)
            }

            PeriodType.WEEK -> {
                val today = LocalDate.now()
                val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                val sunday = monday.plusDays(6)

                val startOfWeek = LocalDateTime.of(monday, LocalTime.MIN)
                val endOfWeek = LocalDateTime.of(sunday, LocalTime.MAX)
                Pair(startOfWeek, endOfWeek)
            }

            PeriodType.MONTH -> {
                val firstDayOfMonth = LocalDate.now().withDayOfMonth(1)
                val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

                val startOfMonth = LocalDateTime.of(firstDayOfMonth, LocalTime.MIN)
                val endOfMonth = LocalDateTime.of(lastDayOfMonth, LocalTime.MAX)
                Pair(startOfMonth, endOfMonth)
            }
        }
    }

    private fun getPreviousPeriodDates(periodType: PeriodType): Pair<LocalDateTime, LocalDateTime> {
        return when (periodType) {
            PeriodType.TODAY -> {
                val yesterday = LocalDate.now().minusDays(1)
                val startOfDay = LocalDateTime.of(yesterday, LocalTime.MIN)
                val endOfDay = LocalDateTime.of(yesterday, LocalTime.MAX)
                Pair(startOfDay, endOfDay)
            }

            PeriodType.WEEK -> {
                val today = LocalDate.now()
                val lastMonday = today.minusDays(today.dayOfWeek.value.toLong() + 6)
                val lastSunday = lastMonday.plusDays(6)

                val startOfWeek = LocalDateTime.of(lastMonday, LocalTime.MIN)
                val endOfWeek = LocalDateTime.of(lastSunday, LocalTime.MAX)
                Pair(startOfWeek, endOfWeek)
            }

            PeriodType.MONTH -> {
                val firstDayOfLastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1)
                val lastDayOfLastMonth = firstDayOfLastMonth.plusMonths(1).minusDays(1)

                val startOfMonth = LocalDateTime.of(firstDayOfLastMonth, LocalTime.MIN)
                val endOfMonth = LocalDateTime.of(lastDayOfLastMonth, LocalTime.MAX)
                Pair(startOfMonth, endOfMonth)
            }
        }
    }

    private enum class PeriodType {
        TODAY,
        WEEK,
        MONTH
    }
}
