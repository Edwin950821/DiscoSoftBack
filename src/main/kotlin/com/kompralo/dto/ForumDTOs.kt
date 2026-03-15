package com.kompralo.dto

import com.kompralo.model.ForumCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateForumPostRequest(
    @field:NotBlank(message = "El titulo es requerido")
    @field:Size(max = 255, message = "El titulo no puede exceder 255 caracteres")
    val title: String,
    @field:NotBlank(message = "El contenido es requerido")
    @field:Size(max = 10000, message = "El contenido no puede exceder 10000 caracteres")
    val content: String,
    val category: ForumCategory = ForumCategory.GENERAL,
    @field:Size(max = 5, message = "Maximo 5 imagenes")
    val imageUrls: List<String>? = null
)

data class UpdateForumPostRequest(
    @field:Size(max = 255, message = "El titulo no puede exceder 255 caracteres")
    val title: String? = null,
    @field:Size(max = 10000, message = "El contenido no puede exceder 10000 caracteres")
    val content: String? = null,
    val category: ForumCategory? = null
)

data class CreateForumReplyRequest(
    @field:NotBlank(message = "El contenido es requerido")
    @field:Size(max = 5000, message = "La respuesta no puede exceder 5000 caracteres")
    val content: String,
    val parentReplyId: Long? = null,
    @field:Size(max = 5, message = "Maximo 5 imagenes")
    val imageUrls: List<String>? = null
)

data class ForumAuthorResponse(
    val id: Long,
    val name: String,
    val accountType: String,
    val picture: String?
)

data class ForumReplyResponse(
    val id: Long,
    val author: ForumAuthorResponse,
    val content: String,
    val parentReplyId: Long?,
    val parentAuthorName: String?,
    val imageUrls: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ReplyPreviewResponse(
    val id: Long,
    val author: ForumAuthorResponse,
    val content: String,
    val createdAt: LocalDateTime
)

data class ForumPostSummaryResponse(
    val id: Long,
    val title: String,
    val excerpt: String,
    val author: ForumAuthorResponse,
    val category: ForumCategory,
    val viewCount: Long,
    val replyCount: Long,
    val likeCount: Long,
    val likedByCurrentUser: Boolean,
    val imageUrls: List<String>,
    val latestReplies: List<ReplyPreviewResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ForumPostDetailResponse(
    val id: Long,
    val title: String,
    val content: String,
    val author: ForumAuthorResponse,
    val category: ForumCategory,
    val viewCount: Long,
    val replyCount: Long,
    val likeCount: Long,
    val likedByCurrentUser: Boolean,
    val replies: List<ForumReplyResponse>,
    val imageUrls: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ForumPostPageResponse(
    val posts: List<ForumPostSummaryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

data class ForumLikeResponse(
    val postId: Long,
    val liked: Boolean,
    val likeCount: Long
)
