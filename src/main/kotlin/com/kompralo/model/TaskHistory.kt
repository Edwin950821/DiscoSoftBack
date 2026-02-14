package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class TaskHistoryAction {
    CREATED, UPDATED, STATUS_CHANGED, ASSIGNED, COMMENTED, COMPLETED, SUBTASK_ADDED, SUBTASK_COMPLETED, LABEL_ADDED, LABEL_REMOVED
}

@Entity
@Table(name = "task_history")
data class TaskHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    val task: Task,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val action: TaskHistoryAction,

    @Column(length = 100)
    val field: String? = null,

    @Column(columnDefinition = "TEXT")
    val oldValue: String? = null,

    @Column(columnDefinition = "TEXT")
    val newValue: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
