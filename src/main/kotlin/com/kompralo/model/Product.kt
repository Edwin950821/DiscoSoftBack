package com.kompralo.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class ProductStatus {
    ACTIVE,
    OUT_OF_STOCK,
    INACTIVE
}

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Column(nullable = false)
    var name: String,

    @Column(unique = true, nullable = false, length = 50)
    var sku: String,

    @Column(nullable = false, length = 100)
    var category: String,

    @Column(nullable = false, precision = 15, scale = 2)
    var price: BigDecimal,

    @Column(nullable = false)
    var stock: Int = 0,

    @Column(nullable = false)
    var sales: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ProductStatus = ProductStatus.ACTIVE,

    @Column(length = 500)
    var imageUrl: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun restock(quantity: Int) {
        stock += quantity
        if (stock > 0 && status == ProductStatus.OUT_OF_STOCK) {
            status = ProductStatus.ACTIVE
        }
    }

    fun updateStockStatus() {
        if (stock <= 0) {
            status = ProductStatus.OUT_OF_STOCK
        }
    }
}
