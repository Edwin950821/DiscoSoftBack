package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_delivery_log")
data class EmailDeliveryLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val offerId: Long,

    @Column(nullable = false)
    val offerTitle: String,

    @Column(nullable = false, length = 30)
    val emailType: String,

    @Column(nullable = false)
    val recipientEmail: String,

    @Column(nullable = false, length = 20)
    val status: String = "SENT",

    @Column(nullable = false, updatable = false)
    val sentAt: LocalDateTime = LocalDateTime.now()
)
