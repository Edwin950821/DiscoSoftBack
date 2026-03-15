package com.kompralo.services

import com.kompralo.dto.TaskMetricsResponse
import com.kompralo.model.TaskPriority
import com.kompralo.model.TaskStatus
import com.kompralo.model.User
import com.kompralo.repository.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class TaskMetricsService(
    private val taskRepository: TaskRepository
) {

    fun getMetrics(user: User): TaskMetricsResponse {
        val now = LocalDateTime.now()
        val startOfDay = now.with(LocalTime.MIN)
        val endOfDay = now.with(LocalTime.MAX)

        val allTasks = taskRepository.findByCreatedByOrderByCreatedAtDesc(user)
        val total = allTasks.size.toLong()
        val todo = allTasks.count { it.status == TaskStatus.TODO }.toLong()
        val inProgress = allTasks.count { it.status == TaskStatus.IN_PROGRESS }.toLong()
        val done = allTasks.count { it.status == TaskStatus.DONE }.toLong()
        val overdue = allTasks.count {
            it.dueDate != null && it.dueDate!!.isBefore(now) && it.status != TaskStatus.DONE
        }.toLong()

        val completedToday = allTasks.count {
            it.status == TaskStatus.DONE &&
            it.completedAt != null &&
            it.completedAt!!.isAfter(startOfDay) &&
            it.completedAt!!.isBefore(endOfDay)
        }.toLong()

        val highPriority = allTasks.count {
            it.priority == TaskPriority.HIGH || it.priority == TaskPriority.URGENT
        }.toLong()

        return TaskMetricsResponse(
            total = total,
            todo = todo,
            inProgress = inProgress,
            done = done,
            overdue = overdue,
            completedToday = completedToday,
            highPriority = highPriority
        )
    }
}
