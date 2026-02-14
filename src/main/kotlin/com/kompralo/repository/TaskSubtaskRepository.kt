package com.kompralo.repository

import com.kompralo.model.Task
import com.kompralo.model.TaskSubtask
import org.springframework.data.jpa.repository.JpaRepository

interface TaskSubtaskRepository : JpaRepository<TaskSubtask, Long> {
    fun findByTaskOrderBySortOrderAsc(task: Task): List<TaskSubtask>
    fun countByTaskAndCompleted(task: Task, completed: Boolean): Long
}
