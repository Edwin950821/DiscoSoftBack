package com.kompralo.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ForumService(
    private val forumPostRepository: ForumPostRepository,
    private val forumReplyRepository: ForumReplyRepository,
    private val forumLikeRepository: ForumLikeRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {

    companion object {
        const val MAX_TITLE_LENGTH = 255
        const val MAX_CONTENT_LENGTH = 10000
        const val MAX_REPLY_LENGTH = 5000
        const val MAX_PAGE_SIZE = 50
        const val MAX_IMAGES = 5
    }

    private val objectMapper = jacksonObjectMapper()

    private fun serializeImageUrls(urls: List<String>?): String? {
        if (urls.isNullOrEmpty()) return null
        return objectMapper.writeValueAsString(urls.take(MAX_IMAGES))
    }

    private fun parseImageUrls(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            objectMapper.readValue(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getPosts(
        category: ForumCategory?,
        search: String?,
        page: Int,
        size: Int,
        currentUser: User?
    ): ForumPostPageResponse {
        val clampedSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page, clampedSize)
        val pageResult = when {
            category != null && !search.isNullOrBlank() ->
                forumPostRepository.findByCategoryAndSearch(category, search, pageable)
            category != null ->
                forumPostRepository.findByCategory(category, pageable)
            !search.isNullOrBlank() ->
                forumPostRepository.findBySearch(search, pageable)
            else ->
                forumPostRepository.findAllPosts(pageable)
        }
        return ForumPostPageResponse(
            posts = pageResult.content.map { it.toSummary(currentUser) },
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
            currentPage = page
        )
    }

    @Transactional
    fun getPostDetail(id: Long, currentUser: User?): ForumPostDetailResponse {
        val post = forumPostRepository.findById(id)
            .orElseThrow { RuntimeException("Post no encontrado") }
        forumPostRepository.incrementViewCount(id)
        val replies = forumReplyRepository.findByPostOrderByCreatedAtAsc(post)
        return post.toDetail(replies, currentUser, post.viewCount + 1)
    }

    @Transactional
    fun createPost(request: CreateForumPostRequest, user: User): ForumPostDetailResponse {
        if (request.title.isBlank()) throw RuntimeException("El titulo no puede estar vacio")
        if (request.content.isBlank()) throw RuntimeException("El contenido no puede estar vacio")
        if (request.title.length > MAX_TITLE_LENGTH) throw RuntimeException("El titulo no puede exceder $MAX_TITLE_LENGTH caracteres")
        if (request.content.length > MAX_CONTENT_LENGTH) throw RuntimeException("El contenido no puede exceder $MAX_CONTENT_LENGTH caracteres")
        val post = ForumPost(
            title = request.title.trim(),
            content = request.content.trim(),
            author = user,
            category = request.category,
            imageUrls = serializeImageUrls(request.imageUrls)
        )
        val saved = forumPostRepository.save(post)
        return saved.toDetail(emptyList(), user, 0)
    }

    @Transactional
    fun updatePost(id: Long, request: UpdateForumPostRequest, user: User): ForumPostDetailResponse {
        val post = forumPostRepository.findById(id)
            .orElseThrow { RuntimeException("Post no encontrado") }
        if (post.author.id != user.id) throw RuntimeException("No tienes permiso para editar este post")
        request.title?.let {
            if (it.length > MAX_TITLE_LENGTH) throw RuntimeException("El titulo no puede exceder $MAX_TITLE_LENGTH caracteres")
            post.title = it.trim()
        }
        request.content?.let {
            if (it.length > MAX_CONTENT_LENGTH) throw RuntimeException("El contenido no puede exceder $MAX_CONTENT_LENGTH caracteres")
            post.content = it.trim()
        }
        request.category?.let { post.category = it }
        val saved = forumPostRepository.save(post)
        val replies = forumReplyRepository.findByPostOrderByCreatedAtAsc(saved)
        return saved.toDetail(replies, user, saved.viewCount)
    }

    @Transactional
    fun deletePost(id: Long, user: User) {
        val post = forumPostRepository.findById(id)
            .orElseThrow { RuntimeException("Post no encontrado") }
        if (post.author.id != user.id) throw RuntimeException("No tienes permiso para eliminar este post")
        forumPostRepository.delete(post)
    }

    @Transactional
    fun createReply(postId: Long, request: CreateForumReplyRequest, user: User): ForumReplyResponse {
        if (request.content.isBlank()) throw RuntimeException("La respuesta no puede estar vacia")
        if (request.content.length > MAX_REPLY_LENGTH) throw RuntimeException("La respuesta no puede exceder $MAX_REPLY_LENGTH caracteres")
        val post = forumPostRepository.findById(postId)
            .orElseThrow { RuntimeException("Post no encontrado") }
        val parentReply = request.parentReplyId?.let {
            val parent = forumReplyRepository.findById(it)
                .orElseThrow { RuntimeException("Respuesta padre no encontrada") }
            if (parent.post.id != post.id) throw RuntimeException("La respuesta padre no pertenece a este post")
            parent
        }
        val reply = ForumReply(
            post = post,
            author = user,
            content = request.content.trim(),
            parentReply = parentReply,
            imageUrls = serializeImageUrls(request.imageUrls)
        )
        val saved = forumReplyRepository.save(reply)

        if (post.author.id != user.id) {
            try {
                notificationService.createAndSend(
                    userId = post.author.id!!,
                    type = NotificationType.MESSAGE_RECEIVED,
                    title = "Nueva respuesta en tu post",
                    message = "${user.name} respondio a \"${post.title.take(60)}\"",
                    priority = "low",
                    actionUrl = "/admin/community",
                    relatedEntityId = post.id,
                    relatedEntityType = RelatedEntityType.FORUM_POST
                )
            } catch (_: Exception) { }
        }

        if (parentReply != null && parentReply.author.id != user.id && parentReply.author.id != post.author.id) {
            try {
                notificationService.createAndSend(
                    userId = parentReply.author.id!!,
                    type = NotificationType.MESSAGE_RECEIVED,
                    title = "Respondieron a tu comentario",
                    message = "${user.name} respondio a tu comentario en \"${post.title.take(60)}\"",
                    priority = "low",
                    actionUrl = "/admin/community",
                    relatedEntityId = post.id,
                    relatedEntityType = RelatedEntityType.FORUM_POST
                )
            } catch (_: Exception) { }
        }

        return saved.toResponse()
    }

    @Transactional
    fun deleteReply(replyId: Long, user: User) {
        val reply = forumReplyRepository.findById(replyId)
            .orElseThrow { RuntimeException("Respuesta no encontrada") }
        if (reply.author.id != user.id) throw RuntimeException("No tienes permiso para eliminar esta respuesta")
        forumReplyRepository.nullifyParentReply(replyId)
        forumReplyRepository.delete(reply)
    }

    @Transactional
    fun toggleLike(postId: Long, user: User): ForumLikeResponse {
        val post = forumPostRepository.findById(postId)
            .orElseThrow { RuntimeException("Post no encontrado") }
        val existing = forumLikeRepository.findByPostAndUser(post, user)
        val liked = if (existing != null) {
            forumLikeRepository.delete(existing)
            forumLikeRepository.flush()
            false
        } else {
            try {
                forumLikeRepository.save(ForumLike(post = post, user = user))
                true
            } catch (_: DataIntegrityViolationException) {
                false
            }
        }
        val count = forumLikeRepository.countByPost(post)
        return ForumLikeResponse(postId = postId, liked = liked, likeCount = count)
    }

    private fun User.toAuthor() = ForumAuthorResponse(
        id = id!!,
        name = name,
        accountType = if (role == Role.BUSINESS || role == Role.ADMIN) "store" else "buyer",
        picture = image
    )

    private fun ForumPost.toSummary(currentUser: User?) = ForumPostSummaryResponse(
        id = id!!,
        title = title,
        excerpt = content.take(200),
        author = author.toAuthor(),
        category = category,
        viewCount = viewCount,
        replyCount = forumReplyRepository.countByPost(this),
        likeCount = forumLikeRepository.countByPost(this),
        likedByCurrentUser = currentUser?.let { u -> forumLikeRepository.existsByPostAndUser(this, u) } ?: false,
        imageUrls = parseImageUrls(imageUrls),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ForumPost.toDetail(
        replyList: List<ForumReply>,
        currentUser: User?,
        currentViewCount: Long
    ) = ForumPostDetailResponse(
        id = id!!,
        title = title,
        content = content,
        author = author.toAuthor(),
        category = category,
        viewCount = currentViewCount,
        replyCount = replyList.size.toLong(),
        likeCount = forumLikeRepository.countByPost(this),
        likedByCurrentUser = currentUser?.let { u -> forumLikeRepository.existsByPostAndUser(this, u) } ?: false,
        replies = replyList.map { it.toResponse() },
        imageUrls = parseImageUrls(imageUrls),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ForumReply.toResponse() = ForumReplyResponse(
        id = id!!,
        author = author.toAuthor(),
        content = content,
        parentReplyId = parentReply?.id,
        parentAuthorName = parentReply?.author?.name,
        imageUrls = parseImageUrls(imageUrls),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
