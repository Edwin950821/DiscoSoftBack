package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.*
import com.kompralo.mapper.TaskMapper
import com.kompralo.model.*
import com.kompralo.port.NotificationPort
import com.kompralo.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskCommentRepository: TaskCommentRepository,
    private val taskLabelRepository: TaskLabelRepository,
    private val taskHistoryRepository: TaskHistoryRepository,
    private val userRepository: UserRepository,
    private val notificationPort: NotificationPort,
    private val taskMapper: TaskMapper
) {

    fun getTasks(
        user: User,
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
        search: String? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null
    ): TaskListResponse {
        var tasks = taskRepository.findByCreatedByOrderByCreatedAtDesc(user)

        status?.let { s -> tasks = tasks.filter { it.status == s } }
        priority?.let { p -> tasks = tasks.filter { it.priority == p } }
        search?.let { q -> tasks = tasks.filter { it.title.contains(q, ignoreCase = true) } }
        startDate?.let { sd -> tasks = tasks.filter { it.dueDate != null && it.dueDate!! >= sd } }
        endDate?.let { ed -> tasks = tasks.filter { it.dueDate != null && it.dueDate!! <= ed } }

        return TaskListResponse(
            tasks = tasks.map { taskMapper.toResponse(it) },
            total = tasks.size
        )
    }

    fun getTaskById(id: Long, user: User): TaskResponse {
        val task = taskRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tarea", id) }
        if (task.createdBy.id != user.id && task.assignedTo?.id != user.id) {
            throw UnauthorizedActionException("No tienes acceso a esta tarea")
        }
        return taskMapper.toResponse(task)
    }

    @Transactional
    fun createTask(request: CreateTaskRequest, user: User): TaskResponse {
        val assignedTo = request.assignedToId?.let {
            userRepository.findById(it).orElse(null)
        }

        val labels = if (request.labelIds.isNotEmpty()) {
            taskLabelRepository.findAllById(request.labelIds).toMutableSet()
        } else mutableSetOf()

        val task = Task(
            title = request.title,
            description = request.description,
            status = request.status,
            priority = request.priority,
            dueDate = request.dueDate,
            startDate = request.startDate,
            endDate = request.endDate,
            category = request.category,
            assignedTo = assignedTo,
            createdBy = user,
            labels = labels,
            relatedOrderId = request.relatedOrderId,
            relatedProductId = request.relatedProductId,
            relatedCustomerId = request.relatedCustomerId,
            isRecurring = request.isRecurring,
            recurringPattern = request.recurringPattern
        )

        val saved = taskRepository.save(task)
        addHistory(saved, user, TaskHistoryAction.CREATED, null, null, "Tarea creada")

        if (saved.assignedTo != null && saved.assignedTo!!.id != user.id) {
            notificationPort.createAndSend(
                userId = saved.assignedTo!!.id!!,
                type = NotificationType.TASK_ASSIGNED,
                title = "Nueva tarea asignada",
                message = "'${saved.title}' te ha sido asignada por ${user.name}.",
                priority = "medium",
                actionUrl = "/admin/tasks",
                relatedEntityId = saved.id,
                relatedEntityType = RelatedEntityType.TASK
            )
        }

        return taskMapper.toResponse(saved)
    }

    @Transactional
    fun updateTask(id: Long, request: UpdateTaskRequest, user: User): TaskResponse {
        val task = taskRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tarea", id) }

        if (task.createdBy.id != user.id) {
            throw UnauthorizedActionException("No tienes permiso para editar esta tarea")
        }

        request.title?.let {
            if (it != task.title) {
                addHistory(task, user, TaskHistoryAction.UPDATED, "title", task.title, it)
                task.title = it
            }
        }
        request.description?.let { task.description = it }
        request.status?.let {
            if (it != task.status) {
                val oldStatus = task.status.name
                addHistory(task, user, TaskHistoryAction.STATUS_CHANGED, "status", oldStatus, it.name)
                task.status = it
                if (it == TaskStatus.DONE) {
                    task.completedAt = LocalDateTime.now()
                    addHistory(task, user, TaskHistoryAction.COMPLETED, null, null, null)
                } else {
                    task.completedAt = null
                }
                notifyStatusChange(task, user, oldStatus, it.name)
            }
        }
        request.priority?.let {
            if (it != task.priority) {
                addHistory(task, user, TaskHistoryAction.UPDATED, "priority", task.priority.name, it.name)
                task.priority = it
            }
        }
        request.dueDate?.let { task.dueDate = it }
        request.startDate?.let { task.startDate = it }
        request.endDate?.let { task.endDate = it }
        request.category?.let { task.category = it }
        request.assignedToId?.let { assignedId ->
            val newAssigned = userRepository.findById(assignedId).orElse(null)
            if (newAssigned != null && newAssigned.id != task.assignedTo?.id) {
                addHistory(task, user, TaskHistoryAction.ASSIGNED, "assignedTo", task.assignedTo?.name, newAssigned.name)
                task.assignedTo = newAssigned
                if (newAssigned.id != user.id) {
                    notificationPort.createAndSend(
                        userId = newAssigned.id!!,
                        type = NotificationType.TASK_ASSIGNED,
                        title = "Tarea asignada",
                        message = "'${task.title}' te ha sido asignada por ${user.name}.",
                        priority = "medium",
                        actionUrl = "/admin/tasks",
                        relatedEntityId = task.id,
                        relatedEntityType = RelatedEntityType.TASK
                    )
                }
            }
        }
        request.labelIds?.let { ids ->
            task.labels = taskLabelRepository.findAllById(ids).toMutableSet()
        }
        request.relatedOrderId?.let { task.relatedOrderId = it }
        request.relatedProductId?.let { task.relatedProductId = it }
        request.relatedCustomerId?.let { task.relatedCustomerId = it }
        request.isRecurring?.let { task.isRecurring = it }
        request.recurringPattern?.let { task.recurringPattern = it }

        task.updatedAt = LocalDateTime.now()
        return taskMapper.toResponse(taskRepository.save(task))
    }

    @Transactional
    fun updateTaskStatus(id: Long, status: TaskStatus, user: User): TaskResponse {
        val task = taskRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tarea", id) }

        val oldStatus = task.status
        task.status = status
        task.updatedAt = LocalDateTime.now()

        if (status == TaskStatus.DONE) {
            task.completedAt = LocalDateTime.now()
        } else {
            task.completedAt = null
        }

        addHistory(task, user, TaskHistoryAction.STATUS_CHANGED, "status", oldStatus.name, status.name)
        notifyStatusChange(task, user, oldStatus.name, status.name)

        return taskMapper.toResponse(taskRepository.save(task))
    }

    @Transactional
    fun deleteTask(id: Long, user: User) {
        val task = taskRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Tarea", id) }
        if (task.createdBy.id != user.id) {
            throw UnauthorizedActionException("No tienes permiso para eliminar esta tarea")
        }
        taskRepository.delete(task)
    }

    fun getComments(taskId: Long): List<CommentResponse> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { EntityNotFoundException("Tarea", taskId) }
        return taskCommentRepository.findByTaskOrderByCreatedAtDesc(task).map { taskMapper.toResponse(it) }
    }

    @Transactional
    fun addComment(taskId: Long, request: CreateCommentRequest, user: User): CommentResponse {
        val task = taskRepository.findById(taskId)
            .orElseThrow { EntityNotFoundException("Tarea", taskId) }

        val comment = TaskComment(
            task = task,
            author = user,
            content = request.content
        )

        val saved = taskCommentRepository.save(comment)
        addHistory(task, user, TaskHistoryAction.COMMENTED, null, null, request.content.take(100))

        val targets = mutableSetOf<Long>()
        if (task.createdBy.id != user.id) targets.add(task.createdBy.id!!)
        if (task.assignedTo != null && task.assignedTo!!.id != user.id) targets.add(task.assignedTo!!.id!!)
        targets.forEach { targetId ->
            notificationPort.createAndSend(
                userId = targetId,
                type = NotificationType.TASK_COMMENTED,
                title = "Nuevo comentario",
                message = "${user.name} comento en '${task.title}': ${request.content.take(80)}",
                priority = "low",
                actionUrl = "/admin/tasks",
                relatedEntityId = task.id,
                relatedEntityType = RelatedEntityType.TASK
            )
        }

        return taskMapper.toResponse(saved)
    }

    @Transactional
    fun deleteComment(taskId: Long, commentId: Long) {
        taskCommentRepository.deleteById(commentId)
    }

    fun getHistory(taskId: Long): List<HistoryResponse> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { EntityNotFoundException("Tarea", taskId) }
        return taskHistoryRepository.findByTaskOrderByCreatedAtDesc(task).map { taskMapper.toResponse(it) }
    }

    private fun notifyStatusChange(task: Task, user: User, oldStatus: String, newStatus: String) {
        val targets = mutableSetOf<Long>()
        if (task.createdBy.id != user.id) targets.add(task.createdBy.id!!)
        if (task.assignedTo != null && task.assignedTo!!.id != user.id) targets.add(task.assignedTo!!.id!!)
        targets.forEach { targetId ->
            notificationPort.createAndSend(
                userId = targetId,
                type = NotificationType.TASK_STATUS_CHANGED,
                title = "Estado de tarea cambiado",
                message = "'${task.title}' cambio de $oldStatus a $newStatus.",
                priority = "medium",
                actionUrl = "/admin/tasks",
                relatedEntityId = task.id,
                relatedEntityType = RelatedEntityType.TASK
            )
        }
    }

    private fun addHistory(
        task: Task,
        user: User,
        action: TaskHistoryAction,
        field: String?,
        oldValue: String?,
        newValue: String?
    ) {
        taskHistoryRepository.save(
            TaskHistory(task = task, user = user, action = action, field = field, oldValue = oldValue, newValue = newValue)
        )
    }
}
