package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "stock_restocks")
data class StockRestock(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val previousStock: Int,

    @Column(nullable = false)
    val newStock: Int,

    @Column(nullable = false)
    val restockDate: LocalDate,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    val batch: StockBatch? = null,

    @Column(precision = 15, scale = 2, columnDefinition = "NUMERIC(15,2) DEFAULT 0")
    var unitCost: BigDecimal = BigDecimal.ZERO,

    var expiryDate: LocalDate? = null,

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    var quantityRemaining: Int = 0,

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    var quantitySold: Int = 0,

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    var quantityDamaged: Int = 0,

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    var quantityReturned: Int = 0,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
