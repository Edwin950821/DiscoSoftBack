package com.kompralo.repository

import com.kompralo.model.Task
import com.kompralo.model.TaskPriority
import com.kompralo.model.TaskStatus
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TaskRepository : JpaRepository<Task, Long> {

    fun findByCreatedByOrderByCreatedAtDesc(createdBy: User): List<Task>

    fun findByAssignedToOrderByDueDateAsc(assignedTo: User): List<Task>

    fun findByStatusAndCreatedBy(status: TaskStatus, createdBy: User): List<Task>

    fun findByPriorityAndCreatedBy(priority: TaskPriority, createdBy: User): List<Task>

    fun findByDueDateBetweenAndCreatedBy(
        start: LocalDateTime,
        end: LocalDateTime,
        createdBy: User
    ): List<Task>

    @Query("""
        SELECT t FROM Task t
        WHERE t.createdBy = :user
        AND (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:startDate IS NULL OR t.dueDate >= :startDate)
        AND (:endDate IS NULL OR t.dueDate <= :endDate)
        ORDER BY t.createdAt DESC
    """)
    fun findFiltered(
        @Param("user") user: User,
        @Param("status") status: TaskStatus?,
        @Param("priority") priority: TaskPriority?,
        @Param("search") search: String?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): List<Task>

    fun findByDueDateBetweenAndStatusNotAndNotifiedDueSoonFalse(
        start: LocalDateTime,
        end: LocalDateTime,
        status: TaskStatus
    ): List<Task>

    fun findByDueDateBeforeAndStatusNotAndNotifiedOverdueFalse(
        dueDate: LocalDateTime,
        status: TaskStatus
    ): List<Task>

    fun countByCreatedByAndStatus(createdBy: User, status: TaskStatus): Long

    fun countByCreatedByAndDueDateBeforeAndStatusNot(
        createdBy: User,
        dueDate: LocalDateTime,
        status: TaskStatus
    ): Long
}
