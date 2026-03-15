package com.kompralo.services

import com.kompralo.dto.CreateLabelRequest
import com.kompralo.dto.LabelResponse
import com.kompralo.mapper.TaskMapper
import com.kompralo.model.TaskLabel
import com.kompralo.model.User
import com.kompralo.repository.TaskLabelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskLabelService(
    private val taskLabelRepository: TaskLabelRepository,
    private val taskMapper: TaskMapper
) {

    fun getLabels(user: User): List<LabelResponse> {
        return taskLabelRepository.findByCreatedBy(user).map { taskMapper.toResponse(it) }
    }

    @Transactional
    fun createLabel(request: CreateLabelRequest, user: User): LabelResponse {
        val label = TaskLabel(
            name = request.name,
            color = request.color,
            createdBy = user
        )
        return taskMapper.toResponse(taskLabelRepository.save(label))
    }

    @Transactional
    fun deleteLabel(id: Long) {
        taskLabelRepository.deleteById(id)
    }
}
