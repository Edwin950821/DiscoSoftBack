package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskSubtaskRepository: TaskSubtaskRepository,
    private val taskCommentRepository: TaskCommentRepository,
    private val taskLabelRepository: TaskLabelRepository,
    private val taskHistoryRepository: TaskHistoryRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
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
            tasks = tasks.map { it.toResponse() },
            total = tasks.size
        )
    }

    fun getTaskById(id: Long, user: User): TaskResponse {
        val task = taskRepository.findById(id)
            .orElseThrow { RuntimeException("Tarea no encontrada") }
        if (task.createdBy.id != user.id && task.assignedTo?.id != user.id) {
            throw RuntimeException("No tienes acceso a esta tarea")
        }
        return task.toResponse()
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
            notificationService.createAndSend(
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

        return saved.toResponse()
    }

    @Transactional
    fun updateTask(id: Long, request: UpdateTaskRequest, user: User): TaskResponse {
        val task = taskRepository.findById(id)
            .orElseThrow { RuntimeException("Tarea no encontrada") }

        if (task.createdBy.id != user.id) {
            throw RuntimeException("No tienes permiso para editar esta tarea")
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
                val targets = mutableSetOf<Long>()
                if (task.createdBy.id != user.id) targets.add(task.createdBy.id!!)
                if (task.assignedTo != null && task.assignedTo!!.id != user.id) targets.add(task.assignedTo!!.id!!)
                targets.forEach { targetId ->
                    notificationService.createAndSend(
                        userId = targetId,
                        type = NotificationType.TASK_STATUS_CHANGED,
                        title = "Estado de tarea cambiado",
                        message = "'${task.title}' cambio de $oldStatus a ${it.name}.",
                        priority = "medium",
                        actionUrl = "/admin/tasks",
                        relatedEntityId = task.id,
                        relatedEntityType = RelatedEntityType.TASK
                    )
                }
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
                    notificationService.createAndSend(
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
        return taskRepository.save(task).toResponse()
    }

    @Transactional
    fun updateTaskStatus(id: Long, status: TaskStatus, user: User): TaskResponse {
        val task = taskRepository.findById(id)
            .orElseThrow { RuntimeException("Tarea no encontrada") }

        val oldStatus = task.status
        task.status = status
        task.updatedAt = LocalDateTime.now()

        if (status == TaskStatus.DONE) {
            task.completedAt = LocalDateTime.now()
        } else {
            task.completedAt = null
        }

        addHistory(task, user, TaskHistoryAction.STATUS_CHANGED, "status", oldStatus.name, status.name)

        val targets = mutableSetOf<Long>()
        if (task.createdBy.id != user.id) targets.add(task.createdBy.id!!)
        if (task.assignedTo != null && task.assignedTo!!.id != user.id) targets.add(task.assignedTo!!.id!!)
        targets.forEach { targetId ->
            notificationService.createAndSend(
                userId = targetId,
                type = NotificationType.TASK_STATUS_CHANGED,
                title = "Estado de tarea cambiado",
                message = "'${task.title}' cambio de ${oldStatus.name} a ${status.name}.",
                priority = "medium",
                actionUrl = "/admin/tasks",
                relatedEntityId = task.id,
                relatedEntityType = RelatedEntityType.TASK
            )
        }

        return taskRepository.save(task).toResponse()
    }

    @Transactional
    fun deleteTask(id: Long, user: User) {
        val task = taskRepository.findById(id)
            .orElseThrow { RuntimeException("Tarea no encontrada") }
        if (task.createdBy.id != user.id) {
            throw RuntimeException("No tienes permiso para eliminar esta tarea")
        }
        taskRepository.delete(task)
    }


    @Transactional
    fun addSubtask(taskId: Long, request: CreateSubtaskRequest, user: User): SubtaskResponse {
        val task = taskRepository.findById(taskId)
            .orElseThrow { RuntimeException("Tarea no encontrada") }

        val maxOrder = task.subtasks.maxOfOrNull { it.sortOrder } ?: -1
        val subtask = TaskSubtask(
            task = task,
            title = request.title,
            sortOrder = maxOrder + 1
        )

        val saved = taskSubtaskRepository.save(subtask)
        addHistory(task, user, TaskHistoryAction.SUBTASK_ADDED, null, null, request.title)
        return saved.toResponse()
    }

    @Transactional
    fun updateSubtask(taskId: Long, subtaskId: Long, request: UpdateSubtaskRequest, user: User): SubtaskResponse {
        val subtask = taskSubtaskRepository.findById(subtaskId)
            .orElseThrow { RuntimeException("Subtarea no encontrada") }

        request.title?.let { subtask.title = it }
        request.completed?.let {
            if (it != subtask.completed) {
                subtask.completed = it
                if (it) {
                    addHistory(subtask.task, user, TaskHistoryAction.SUBTASK_COMPLETED, null, null, subtask.title)
                }
            }
        }
        request.sortOrder?.let { subtask.sortOrder = it }

        return taskSubtaskRepository.save(subtask).toResponse()
    }

    @Transactional
    fun deleteSubtask(taskId: Long, subtaskId: Long) {
        taskSubtaskRepository.deleteById(subtaskId)
    }


    fun getComments(taskId: Long): List<CommentResponse> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { RuntimeException("Tarea no encontrada") }
        return taskCommentRepository.findByTaskOrderByCreatedAtDesc(task).map { it.toResponse() }
    }

    @Transactional
    fun addComment(taskId: Long, request: CreateCommentRequest, user: User): CommentResponse {
        val task = taskRepository.findById(taskId)
            .orElseThrow { RuntimeException("Tarea no encontrada") }

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
            notificationService.createAndSend(
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

        return saved.toResponse()
    }

    @Transactional
    fun deleteComment(taskId: Long, commentId: Long) {
        taskCommentRepository.deleteById(commentId)
    }



    fun getHistory(taskId: Long): List<HistoryResponse> {
        val task = taskRepository.findById(taskId)
            .orElseThrow { RuntimeException("Tarea no encontrada") }
        return taskHistoryRepository.findByTaskOrderByCreatedAtDesc(task).map { it.toResponse() }
    }



    fun getLabels(user: User): List<LabelResponse> {
        return taskLabelRepository.findByCreatedBy(user).map { it.toResponse() }
    }

    @Transactional
    fun createLabel(request: CreateLabelRequest, user: User): LabelResponse {
        val label = TaskLabel(
            name = request.name,
            color = request.color,
            createdBy = user
        )
        return taskLabelRepository.save(label).toResponse()
    }

    @Transactional
    fun deleteLabel(id: Long) {
        taskLabelRepository.deleteById(id)
    }


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

    private fun addHistory(
        task: Task,
        user: User,
        action: TaskHistoryAction,
        field: String?,
        oldValue: String?,
        newValue: String?
    ) {
        val history = TaskHistory(
            task = task,
            user = user,
            action = action,
            field = field,
            oldValue = oldValue,
            newValue = newValue
        )
        taskHistoryRepository.save(history)
    }

    private fun Task.toResponse(): TaskResponse {
        val completedSubtasks = subtasks.count { it.completed }
        val totalSubtasks = subtasks.size
        return TaskResponse(
            id = id!!,
            title = title,
            description = description,
            status = status,
            priority = priority,
            dueDate = dueDate,
            startDate = startDate,
            endDate = endDate,
            category = category,
            assignedTo = assignedTo?.let { TaskUserResponse(it.id!!, it.name, it.email) },
            createdBy = TaskUserResponse(createdBy.id!!, createdBy.name, createdBy.email),
            labels = labels.map { it.toResponse() },
            subtasks = subtasks.map { it.toResponse() },
            subtaskProgress = SubtaskProgressResponse(
                total = totalSubtasks,
                completed = completedSubtasks,
                percentage = if (totalSubtasks > 0) (completedSubtasks * 100 / totalSubtasks) else 0
            ),
            commentCount = comments.size,
            relatedOrderId = relatedOrderId,
            relatedProductId = relatedProductId,
            relatedCustomerId = relatedCustomerId,
            isRecurring = isRecurring,
            recurringPattern = recurringPattern,
            completedAt = completedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun TaskSubtask.toResponse() = SubtaskResponse(
        id = id!!,
        title = title,
        completed = completed,
        sortOrder = sortOrder,
        createdAt = createdAt
    )

    private fun TaskComment.toResponse() = CommentResponse(
        id = id!!,
        author = TaskUserResponse(author.id!!, author.name, author.email),
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TaskLabel.toResponse() = LabelResponse(
        id = id!!,
        name = name,
        color = color
    )

    private fun TaskHistory.toResponse() = HistoryResponse(
        id = id!!,
        user = TaskUserResponse(user.id!!, user.name, user.email),
        action = action,
        field = field,
        oldValue = oldValue,
        newValue = newValue,
        createdAt = createdAt
    )
}
