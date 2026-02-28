package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(name = "auth_users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // VigXa-specific fields (nullable, added for VigXa registration)
    @Column(unique = true)
    val uuid: String? = UUID.randomUUID().toString(),

    @Column
    val code: String? = null,

    @Column(name = "username", unique = true)
    val username: String? = null,

    @Column(name = "company_uuid")
    val companyUuid: String? = null,

    @Column(name = "company_name")
    val companyName: String? = null,

    @Column(name = "company_email")
    val companyEmail: String? = null,

    @Column(name = "company_phone")
    val companyPhone: String? = null,

    @Column(name = "company_nit")
    val companyNit: String? = null,

    @Column(name = "owner_name")
    val ownerName: String? = null,

    @Column(name = "owner_lastname")
    val ownerLastname: String? = null,

    @Column(name = "owner_email")
    val ownerEmail: String? = null,

    @Column(name = "image")
    val image: String? = null,

    @Column(name = "employee_uuid")
    val employeeUuid: String? = null,

    @Column(name = "plan")
    val plan: String? = null,

    @Column(name = "default_currency")
    val defaultCurrency: String? = null
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}


enum class Role {
    USER,
    BUSINESS,
    ADMIN,
    OWNER,
    MANAGER,
    ACCOUNTANT,
    VIEWER
}
