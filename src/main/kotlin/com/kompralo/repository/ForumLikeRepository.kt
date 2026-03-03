package com.kompralo.repository

import com.kompralo.model.ForumLike
import com.kompralo.model.ForumPost
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface ForumLikeRepository : JpaRepository<ForumLike, Long> {
    fun findByPostAndUser(post: ForumPost, user: User): ForumLike?
    fun countByPost(post: ForumPost): Long
    fun existsByPostAndUser(post: ForumPost, user: User): Boolean
}
