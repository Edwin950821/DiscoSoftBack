package com.kompralo.controller

import com.kompralo.dto.CreateForumPostRequest
import com.kompralo.dto.CreateForumReplyRequest
import com.kompralo.dto.UpdateForumPostRequest
import com.kompralo.model.ForumCategory
import com.kompralo.repository.UserRepository
import com.kompralo.services.ForumService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class ForumController(
    private val forumService: ForumService,
    private val userRepository: UserRepository
) {
    private fun getUser(auth: Authentication) =
        userRepository.findByEmail(auth.name)
            .orElseThrow { RuntimeException("Usuario no encontrado") }

    @GetMapping("/api/public/forum/posts")
    fun getPostsPublic(
        @RequestParam category: ForumCategory? = null,
        @RequestParam search: String? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ResponseEntity.ok(forumService.getPosts(category, search, page, size, null))

    @GetMapping("/api/public/forum/posts/{id}")
    fun getPostPublic(@PathVariable id: Long) =
        ResponseEntity.ok(forumService.getPostDetail(id, null))

    @GetMapping("/api/forum/posts")
    fun getPosts(
        auth: Authentication,
        @RequestParam category: ForumCategory? = null,
        @RequestParam search: String? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ResponseEntity.ok(forumService.getPosts(category, search, page, size, getUser(auth)))

    @GetMapping("/api/forum/posts/{id}")
    fun getPost(auth: Authentication, @PathVariable id: Long) =
        ResponseEntity.ok(forumService.getPostDetail(id, getUser(auth)))

    @PostMapping("/api/forum/posts")
    fun createPost(auth: Authentication, @Valid @RequestBody request: CreateForumPostRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(forumService.createPost(request, getUser(auth)))

    @PutMapping("/api/forum/posts/{id}")
    fun updatePost(auth: Authentication, @PathVariable id: Long, @Valid @RequestBody request: UpdateForumPostRequest) =
        ResponseEntity.ok(forumService.updatePost(id, request, getUser(auth)))

    @DeleteMapping("/api/forum/posts/{id}")
    fun deletePost(auth: Authentication, @PathVariable id: Long): ResponseEntity<Void> {
        forumService.deletePost(id, getUser(auth))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/forum/posts/{postId}/replies")
    fun createReply(auth: Authentication, @PathVariable postId: Long, @Valid @RequestBody request: CreateForumReplyRequest) =
        ResponseEntity.status(HttpStatus.CREATED).body(forumService.createReply(postId, request, getUser(auth)))

    @DeleteMapping("/api/forum/replies/{replyId}")
    fun deleteReply(auth: Authentication, @PathVariable replyId: Long): ResponseEntity<Void> {
        forumService.deleteReply(replyId, getUser(auth))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/forum/posts/{postId}/like")
    fun toggleLike(auth: Authentication, @PathVariable postId: Long) =
        ResponseEntity.ok(forumService.toggleLike(postId, getUser(auth)))
}
