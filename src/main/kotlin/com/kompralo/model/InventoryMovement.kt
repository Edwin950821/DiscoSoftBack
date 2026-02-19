package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "inventory_movements",
    indexes = [
        Index(name = "idx_inv_mov_product", columnList = "product_id"),
        Index(name = "idx_inv_mov_restock", columnList = "restock_id"),
        Index(name = "idx_inv_mov_created", columnList = "created_at"),
    ]
)
data class InventoryMovement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restock_id")
    val restock: StockRestock? = null,

    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    val movementType: String,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val resultingStock: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(columnDefinition = "VARCHAR(30)")
    val referenceType: String? = null,

    val referenceId: Long? = null,

    @Column(length = 50)
    val reason: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
