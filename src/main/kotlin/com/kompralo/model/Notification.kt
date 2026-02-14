package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class NotificationType {
   
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    STOCK_RESTOCKED,

   
    LOW_STOCK,
    OUT_OF_STOCK,

  
    NEW_ORDER,
    ORDER_PAID,


    NEW_REVIEW,
    WITHDRAWAL_READY,
    ACCOUNT_VERIFICATION,

 
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,

    
    PRICE_DROP,
    BACK_IN_STOCK,
    PROMO_OFFER,

    PAYMENT_SUCCESS,
    PAYMENT_FAILED,

    TASK_ASSIGNED,
    TASK_STATUS_CHANGED,
    TASK_COMMENTED,
    TASK_DUE_SOON,
    TASK_OVERDUE,

   
    MESSAGE_RECEIVED,
    SYSTEM_UPDATE,
    WELCOME
}

enum class RelatedEntityType {
    PRODUCT,
    TASK,
    ORDER,
    USER
}

@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: NotificationType,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Column(nullable = false)
    var isRead: Boolean = false,

    @Column(length = 10)
    val priority: String = "medium",

    @Column(length = 500)
    val actionUrl: String? = null,

    val relatedEntityId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val relatedEntityType: RelatedEntityType? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
