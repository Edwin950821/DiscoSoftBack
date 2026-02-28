package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "store_followers",
    uniqueConstraints = [UniqueConstraint(columnNames = ["buyer_id", "seller_id"])]
)
data class StoreFollower(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    val buyer: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
