package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class PolicyType { TERMS, PRIVACY, RETURNS, WARRANTY }

@Entity
@Table(name = "store_policies")
data class StorePolicy(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    val seller: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var policyType: PolicyType,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(nullable = false)
    var active: Boolean = true,

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
