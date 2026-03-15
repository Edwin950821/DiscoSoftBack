package com.kompralo.services

import com.kompralo.model.NotificationType
import com.kompralo.model.RelatedEntityType
import com.kompralo.model.TaskStatus
import com.kompralo.port.NotificationPort
import com.kompralo.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class TaskNotificationScheduler(
    private val taskRepository: TaskRepository,
    private val notificationPort: NotificationPort
) {
    private val log = LoggerFactory.getLogger(TaskNotificationScheduler::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    fun checkTaskDeadlines() {
        log.info("Checking task deadlines for notifications...")
        val now = LocalDateTime.now()

        checkDueSoon(now)
        checkOverdue(now)
    }

    private fun checkDueSoon(now: LocalDateTime) {
        val in24h = now.plusHours(24)

        val dueSoonTasks = taskRepository.findByDueDateBetweenAndStatusNotAndNotifiedDueSoonFalse(
            now, in24h, TaskStatus.DONE
        )

        for (task in dueSoonTasks) {
            val userId = task.createdBy.id ?: continue
            val dueFormatted = task.dueDate?.format(dateFormatter) ?: "pronto"

            notificationPort.createAndSend(
                userId = userId,
                type = NotificationType.TASK_DUE_SOON,
                title = "Tarea a punto de vencer",
                message = "\"${task.title}\" vence el $dueFormatted",
                priority = "high",
                actionUrl = "/admin/tasks",
                relatedEntityId = task.id,
                relatedEntityType = RelatedEntityType.TASK
            )

            task.notifiedDueSoon = true
            taskRepository.save(task)
            log.info("Sent TASK_DUE_SOON notification for task ${task.id}: ${task.title}")
        }

        if (dueSoonTasks.isNotEmpty()) {
            log.info("Sent ${dueSoonTasks.size} due-soon notifications")
        }
    }

    private fun checkOverdue(now: LocalDateTime) {
        val overdueTasks = taskRepository.findByDueDateBeforeAndStatusNotAndNotifiedOverdueFalse(
            now, TaskStatus.DONE
        )

        for (task in overdueTasks) {
            val userId = task.createdBy.id ?: continue
            val dueFormatted = task.dueDate?.format(dateFormatter) ?: ""

            notificationPort.createAndSend(
                userId = userId,
                type = NotificationType.TASK_OVERDUE,
                title = "Tarea vencida",
                message = "\"${task.title}\" venció el $dueFormatted y aún no está completada",
                priority = "urgent",
                actionUrl = "/admin/tasks",
                relatedEntityId = task.id,
                relatedEntityType = RelatedEntityType.TASK
            )

            task.notifiedOverdue = true
            taskRepository.save(task)
            log.info("Sent TASK_OVERDUE notification for task ${task.id}: ${task.title}")
        }

        if (overdueTasks.isNotEmpty()) {
            log.info("Sent ${overdueTasks.size} overdue notifications")
        }
    }
}
