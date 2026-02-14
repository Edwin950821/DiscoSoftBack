package com.kompralo.repository

import com.kompralo.model.Task
import com.kompralo.model.TaskHistory
import org.springframework.data.jpa.repository.JpaRepository

interface TaskHistoryRepository : JpaRepository<TaskHistory, Long> {
    fun findByTaskOrderByCreatedAtDesc(task: Task): List<TaskHistory>
}
