package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "special_days")
data class SpecialDay(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    val seller: User? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(nullable = false)
    var recurring: Boolean = false,

    @Column(length = 500)
    var imageUrl: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
