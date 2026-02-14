package com.kompralo.repository

import com.kompralo.model.Task
import com.kompralo.model.TaskComment
import org.springframework.data.jpa.repository.JpaRepository

interface TaskCommentRepository : JpaRepository<TaskComment, Long> {
    fun findByTaskOrderByCreatedAtDesc(task: Task): List<TaskComment>
}
