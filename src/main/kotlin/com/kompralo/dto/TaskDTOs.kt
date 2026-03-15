package com.kompralo.dto

import com.kompralo.model.TaskPriority
import com.kompralo.model.TaskStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateTaskRequest(
    @field:NotBlank(message = "El titulo es requerido")
    @field:Size(max = 255, message = "El titulo no puede exceder 255 caracteres")
    val title: String,
    @field:Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    @field:Size(max = 100, message = "La categoria no puede exceder 100 caracteres")
    val category: String? = null,
    val assignedToId: Long? = null,
    val labelIds: List<Long> = emptyList(),
    val relatedOrderId: Long? = null,
    val relatedProductId: Long? = null,
    val relatedCustomerId: Long? = null,
    val isRecurring: Boolean = false,
    @field:Size(max = 50, message = "El patron de recurrencia no puede exceder 50 caracteres")
    val recurringPattern: String? = null
)

data class UpdateTaskRequest(
    @field:Size(max = 255, message = "El titulo no puede exceder 255 caracteres")
    val title: String? = null,
    @field:Size(max = 5000, message = "La descripcion no puede exceder 5000 caracteres")
    val description: String? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val dueDate: LocalDateTime? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    @field:Size(max = 100, message = "La categoria no puede exceder 100 caracteres")
    val category: String? = null,
    val assignedToId: Long? = null,
    val labelIds: List<Long>? = null,
    val relatedOrderId: Long? = null,
    val relatedProductId: Long? = null,
    val relatedCustomerId: Long? = null,
    val isRecurring: Boolean? = null,
    @field:Size(max = 50, message = "El patron de recurrencia no puede exceder 50 caracteres")
    val recurringPattern: String? = null
)

data class UpdateTaskStatusRequest(
    val status: TaskStatus
)

data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: LocalDateTime?,
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val category: String?,
    val assignedTo: TaskUserResponse?,
    val createdBy: TaskUserResponse,
    val labels: List<LabelResponse>,
    val subtasks: List<SubtaskResponse>,
    val subtaskProgress: SubtaskProgressResponse,
    val commentCount: Int,
    val relatedOrderId: Long?,
    val relatedProductId: Long?,
    val relatedCustomerId: Long?,
    val isRecurring: Boolean,
    val recurringPattern: String?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class TaskUserResponse(
    val id: Long,
    val name: String,
    val email: String
)

data class SubtaskProgressResponse(
    val total: Int,
    val completed: Int,
    val percentage: Int
)

data class TaskListResponse(
    val tasks: List<TaskResponse>,
    val total: Int
)

data class TaskMetricsResponse(
    val total: Long,
    val todo: Long,
    val inProgress: Long,
    val done: Long,
    val overdue: Long,
    val completedToday: Long,
    val highPriority: Long
)
