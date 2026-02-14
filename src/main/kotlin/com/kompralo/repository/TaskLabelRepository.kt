package com.kompralo.repository

import com.kompralo.model.TaskLabel
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface TaskLabelRepository : JpaRepository<TaskLabel, Long> {
    fun findByCreatedBy(createdBy: User): List<TaskLabel>
}
