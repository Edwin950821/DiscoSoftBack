package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "stock_batches")
data class StockBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false, length = 20)
    val batchNumber: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Column(length = 200)
    var location: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(nullable = false)
    val totalItems: Int,

    @Column(nullable = false)
    val totalQuantity: Int,

    @Column(precision = 15, scale = 2, nullable = false)
    val totalValue: BigDecimal,

    @Column(length = 200)
    var supplier: String? = null,

    @Column(columnDefinition = "VARCHAR(20)")
    var status: String = "ACTIVE",

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
