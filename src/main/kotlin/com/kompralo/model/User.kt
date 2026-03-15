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
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    @Column(name = "is_active")
    var isActive: Boolean? = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

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
    var image: String? = null,

    @Column(name = "employee_uuid")
    val employeeUuid: String? = null,

    @Column(name = "plan")
    val plan: String? = null,

    @Column(name = "default_currency")
    val defaultCurrency: String? = null,

    @Column(name = "auth_provider")
    var authProvider: String? = null,

    @Column(name = "failed_login_attempts")
    var failedLoginAttempts: Int? = 0,

    @Column(name = "locked_until")
    var lockedUntil: LocalDateTime? = null
) {

    @PostLoad
    fun setDefaults() {
        if (failedLoginAttempts == null) failedLoginAttempts = 0
        if (isActive == null) isActive = true
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    fun getFailedAttempts(): Int = failedLoginAttempts ?: 0

    fun isUserActive(): Boolean = isActive ?: true
}

enum class Role {
    USER,
    BUSINESS,
    ADMIN,
    OWNER,
    MANAGER,
    ACCOUNTANT,
    VIEWER,
    MESERO
}
