package com.kompralo.repository

import com.kompralo.model.Notification
import com.kompralo.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<Notification>

    fun countByUserAndIsReadFalse(user: User): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    fun markAllAsReadByUser(@Param("user") user: User): Int

    fun findByIdAndUser(id: Long, user: User): Notification?
}
