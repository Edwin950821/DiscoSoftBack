package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.model.TaskPriority
import com.kompralo.model.TaskStatus
import com.kompralo.repository.UserRepository
import com.kompralo.services.TaskService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class TaskController(
    private val taskService: TaskService,
    private val userRepository: UserRepository
) {

    private fun getUser(auth: Authentication) =
        userRepository.findByEmail(auth.name)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

    // ==================== TASKS ====================

    @GetMapping("/tasks")
    fun getTasks(
        auth: Authentication,
        @RequestParam status: TaskStatus?,
        @RequestParam priority: TaskPriority?,
        @RequestParam search: String?,
        @RequestParam startDate: LocalDateTime?,
        @RequestParam endDate: LocalDateTime?
    ): ResponseEntity<TaskListResponse> {
        val user = getUser(auth)
        val result = taskService.getTasks(user, status, priority, search, startDate, endDate)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/tasks")
    fun createTask(
        auth: Authentication,
        @RequestBody request: CreateTaskRequest
    ): ResponseEntity<TaskResponse> {
        val user = getUser(auth)
        val task = taskService.createTask(request, user)
        return ResponseEntity.status(HttpStatus.CREATED).body(task)
    }

    @GetMapping("/tasks/{id}")
    fun getTask(
        auth: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<TaskResponse> {
        val user = getUser(auth)
        val task = taskService.getTaskById(id, user)
        return ResponseEntity.ok(task)
    }

    @PutMapping("/tasks/{id}")
    fun updateTask(
        auth: Authentication,
        @PathVariable id: Long,
        @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<TaskResponse> {
        val user = getUser(auth)
        val task = taskService.updateTask(id, request, user)
        return ResponseEntity.ok(task)
    }

    @DeleteMapping("/tasks/{id}")
    fun deleteTask(
        auth: Authentication,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val user = getUser(auth)
        taskService.deleteTask(id, user)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/tasks/{id}/status")
    fun updateTaskStatus(
        auth: Authentication,
        @PathVariable id: Long,
        @RequestBody request: UpdateTaskStatusRequest
    ): ResponseEntity<TaskResponse> {
        val user = getUser(auth)
        val task = taskService.updateTaskStatus(id, request.status, user)
        return ResponseEntity.ok(task)
    }

    // ==================== SUBTASKS ====================

    @PostMapping("/tasks/{taskId}/subtasks")
    fun addSubtask(
        auth: Authentication,
        @PathVariable taskId: Long,
        @RequestBody request: CreateSubtaskRequest
    ): ResponseEntity<SubtaskResponse> {
        val user = getUser(auth)
        val subtask = taskService.addSubtask(taskId, request, user)
        return ResponseEntity.status(HttpStatus.CREATED).body(subtask)
    }

    @PatchMapping("/tasks/{taskId}/subtasks/{subtaskId}")
    fun updateSubtask(
        auth: Authentication,
        @PathVariable taskId: Long,
        @PathVariable subtaskId: Long,
        @RequestBody request: UpdateSubtaskRequest
    ): ResponseEntity<SubtaskResponse> {
        val user = getUser(auth)
        val subtask = taskService.updateSubtask(taskId, subtaskId, request, user)
        return ResponseEntity.ok(subtask)
    }

    @DeleteMapping("/tasks/{taskId}/subtasks/{subtaskId}")
    fun deleteSubtask(
        @PathVariable taskId: Long,
        @PathVariable subtaskId: Long
    ): ResponseEntity<Void> {
        taskService.deleteSubtask(taskId, subtaskId)
        return ResponseEntity.noContent().build()
    }

    // ==================== COMMENTS ====================

    @GetMapping("/tasks/{taskId}/comments")
    fun getComments(
        @PathVariable taskId: Long
    ): ResponseEntity<List<CommentResponse>> {
        return ResponseEntity.ok(taskService.getComments(taskId))
    }

    @PostMapping("/tasks/{taskId}/comments")
    fun addComment(
        auth: Authentication,
        @PathVariable taskId: Long,
        @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val user = getUser(auth)
        val comment = taskService.addComment(taskId, request, user)
        return ResponseEntity.status(HttpStatus.CREATED).body(comment)
    }

    @DeleteMapping("/tasks/{taskId}/comments/{commentId}")
    fun deleteComment(
        @PathVariable taskId: Long,
        @PathVariable commentId: Long
    ): ResponseEntity<Void> {
        taskService.deleteComment(taskId, commentId)
        return ResponseEntity.noContent().build()
    }

    // ==================== HISTORY ====================

    @GetMapping("/tasks/{taskId}/history")
    fun getHistory(
        @PathVariable taskId: Long
    ): ResponseEntity<List<HistoryResponse>> {
        return ResponseEntity.ok(taskService.getHistory(taskId))
    }

    // ==================== METRICS ====================

    @GetMapping("/tasks/metrics")
    fun getMetrics(auth: Authentication): ResponseEntity<TaskMetricsResponse> {
        val user = getUser(auth)
        return ResponseEntity.ok(taskService.getMetrics(user))
    }

    // ==================== LABELS ====================

    @GetMapping("/labels")
    fun getLabels(auth: Authentication): ResponseEntity<List<LabelResponse>> {
        val user = getUser(auth)
        return ResponseEntity.ok(taskService.getLabels(user))
    }

    @PostMapping("/labels")
    fun createLabel(
        auth: Authentication,
        @RequestBody request: CreateLabelRequest
    ): ResponseEntity<LabelResponse> {
        val user = getUser(auth)
        val label = taskService.createLabel(request, user)
        return ResponseEntity.status(HttpStatus.CREATED).body(label)
    }

    @DeleteMapping("/labels/{id}")
    fun deleteLabel(@PathVariable id: Long): ResponseEntity<Void> {
        taskService.deleteLabel(id)
        return ResponseEntity.noContent().build()
    }
}
