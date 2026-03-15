package com.kompralo.services

import com.kompralo.dto.CreateSubtaskRequest
import com.kompralo.dto.SubtaskResponse
import com.kompralo.dto.UpdateSubtaskRequest
import com.kompralo.mapper.TaskMapper
import com.kompralo.model.TaskHistoryAction
import com.kompralo.model.TaskSubtask
import com.kompralo.model.User
import com.kompralo.repository.TaskRepository
import com.kompralo.repository.TaskSubtaskRepository
import com.kompralo.repository.TaskHistoryRepository
import com.kompralo.model.TaskHistory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubtaskService(
    private val taskRepository: TaskRepository,
    private val taskSubtaskRepository: TaskSubtaskRepository,
    private val taskHistoryRepository: TaskHistoryRepository,
    private val taskMapper: TaskMapper
) {

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
        return taskMapper.toResponse(saved)
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

        return taskMapper.toResponse(taskSubtaskRepository.save(subtask))
    }

    @Transactional
    fun deleteSubtask(taskId: Long, subtaskId: Long) {
        taskSubtaskRepository.deleteById(subtaskId)
    }

    private fun addHistory(
        task: com.kompralo.model.Task,
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
