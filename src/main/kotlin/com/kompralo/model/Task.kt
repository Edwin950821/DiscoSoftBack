package com.kompralo.model

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDateTime

enum class TaskStatus {
    TODO, IN_PROGRESS, DONE
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Entity
@Table(name = "tasks")
data class Task(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TaskStatus = TaskStatus.TODO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var priority: TaskPriority = TaskPriority.MEDIUM,

    var dueDate: LocalDateTime? = null,
    var startDate: LocalDateTime? = null,
    var endDate: LocalDateTime? = null,

    @Column(length = 100)
    var category: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    var assignedTo: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    val createdBy: User,

    var relatedOrderId: Long? = null,
    var relatedProductId: Long? = null,
    var relatedCustomerId: Long? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_label_assignments",
        joinColumns = [JoinColumn(name = "task_id")],
        inverseJoinColumns = [JoinColumn(name = "label_id")]
    )
    var labels: MutableSet<TaskLabel> = mutableSetOf(),

    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    var subtasks: MutableList<TaskSubtask> = mutableListOf(),

    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("createdAt DESC")
    var comments: MutableList<TaskComment> = mutableListOf(),

    @OneToMany(mappedBy = "task", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("createdAt DESC")
    var history: MutableList<TaskHistory> = mutableListOf(),

    @Column(nullable = false)
    var isRecurring: Boolean = false,

    @Column(length = 100)
    var recurringPattern: String? = null,

    @Column(nullable = false)
    @ColumnDefault("false")
    var notifiedDueSoon: Boolean = false,

    @Column(nullable = false)
    @ColumnDefault("false")
    var notifiedOverdue: Boolean = false,

    var completedAt: LocalDateTime? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
