package com.kompralo.dto

import com.kompralo.model.ForumCategory
import java.time.LocalDateTime

data class CreateForumPostRequest(
    val title: String,
    val content: String,
    val category: ForumCategory = ForumCategory.GENERAL,
    val imageUrls: List<String>? = null
)

data class UpdateForumPostRequest(
    val title: String? = null,
    val content: String? = null,
    val category: ForumCategory? = null
)

data class CreateForumReplyRequest(
    val content: String,
    val parentReplyId: Long? = null,
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
