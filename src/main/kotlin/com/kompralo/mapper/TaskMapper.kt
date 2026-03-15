package com.kompralo.mapper

import com.kompralo.dto.*
import com.kompralo.model.Task
import com.kompralo.model.TaskComment
import com.kompralo.model.TaskHistory
import com.kompralo.model.TaskLabel
import com.kompralo.model.TaskSubtask
import org.springframework.stereotype.Component

@Component
class TaskMapper {

    fun toResponse(task: Task): TaskResponse {
        val completedSubtasks = task.subtasks.count { it.completed }
        val totalSubtasks = task.subtasks.size
        return TaskResponse(
            id = task.id!!,
            title = task.title,
            description = task.description,
            status = task.status,
            priority = task.priority,
            dueDate = task.dueDate,
            startDate = task.startDate,
            endDate = task.endDate,
            category = task.category,
            assignedTo = task.assignedTo?.let { TaskUserResponse(it.id!!, it.name, it.email) },
            createdBy = TaskUserResponse(task.createdBy.id!!, task.createdBy.name, task.createdBy.email),
            labels = task.labels.map { toResponse(it) },
            subtasks = task.subtasks.map { toResponse(it) },
            subtaskProgress = SubtaskProgressResponse(
                total = totalSubtasks,
                completed = completedSubtasks,
                percentage = if (totalSubtasks > 0) (completedSubtasks * 100 / totalSubtasks) else 0
            ),
            commentCount = task.comments.size,
            relatedOrderId = task.relatedOrderId,
            relatedProductId = task.relatedProductId,
            relatedCustomerId = task.relatedCustomerId,
            isRecurring = task.isRecurring,
            recurringPattern = task.recurringPattern,
            completedAt = task.completedAt,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }

    fun toResponse(subtask: TaskSubtask) = SubtaskResponse(
        id = subtask.id!!,
        title = subtask.title,
        completed = subtask.completed,
        sortOrder = subtask.sortOrder,
        createdAt = subtask.createdAt
    )

    fun toResponse(comment: TaskComment) = CommentResponse(
        id = comment.id!!,
        author = TaskUserResponse(comment.author.id!!, comment.author.name, comment.author.email),
        content = comment.content,
        createdAt = comment.createdAt,
        updatedAt = comment.updatedAt
    )

    fun toResponse(label: TaskLabel) = LabelResponse(
        id = label.id!!,
        name = label.name,
        color = label.color
    )

    fun toResponse(history: TaskHistory) = HistoryResponse(
        id = history.id!!,
        user = TaskUserResponse(history.user.id!!, history.user.name, history.user.email),
        action = history.action,
        field = history.field,
        oldValue = history.oldValue,
        newValue = history.newValue,
        createdAt = history.createdAt
    )
}
