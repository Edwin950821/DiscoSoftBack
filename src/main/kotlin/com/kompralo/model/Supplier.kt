package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "suppliers")
data class Supplier(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 30)
    var nit: String? = null,

    @Column(length = 200)
    var contactName: String? = null,

    @Column(length = 100)
    var email: String? = null,

    @Column(length = 30)
    var phone: String? = null,

    @Column(length = 300)
    var address: String? = null,

    @Column(length = 100)
    var city: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT true")
    var isActive: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
