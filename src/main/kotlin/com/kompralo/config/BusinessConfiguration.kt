package com.kompralo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

@ConfigurationProperties(prefix = "kompralo.tax")
data class TaxConfiguration(
    val ivaRate: BigDecimal = BigDecimal("0.19")
)

@ConfigurationProperties(prefix = "kompralo.shipping")
data class ShippingConfiguration(
    val baseRate: BigDecimal = BigDecimal("8000"),
    val codFee: BigDecimal = BigDecimal("5000")
)

@ConfigurationProperties(prefix = "kompralo.finance")
data class FinanceConfiguration(
    val expenseRatio: BigDecimal = BigDecimal("0.65"),
    val cogsRatio: BigDecimal = BigDecimal("0.55"),
    val shippingRatio: BigDecimal = BigDecimal("0.08"),
    val operatingRatio: BigDecimal = BigDecimal("0.05")
)
