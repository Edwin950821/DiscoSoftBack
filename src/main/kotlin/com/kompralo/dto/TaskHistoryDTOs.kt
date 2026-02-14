package com.kompralo.dto

import com.kompralo.model.TaskHistoryAction
import java.time.LocalDateTime

data class HistoryResponse(
    val id: Long,
    val user: TaskUserResponse,
    val action: TaskHistoryAction,
    val field: String?,
    val oldValue: String?,
    val newValue: String?,
    val createdAt: LocalDateTime
)
