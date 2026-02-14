package com.kompralo.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class CustomerResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String,
    val isActive: Boolean,
    val totalOrders: Long,
    val totalSpent: BigDecimal,
    val avgOrderValue: BigDecimal,
    val lastOrderDate: LocalDateTime?,
    val segment: String,
    val registeredAt: LocalDateTime
)

data class CustomerStatsResponse(
    val totalCustomers: Long,
    val activeCustomers: Long,
    val newCustomers: Long,
    val vipCustomers: Long,
    val totalRevenue: BigDecimal,
    val avgCustomerValue: BigDecimal
)
