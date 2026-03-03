package com.kompralo.repository

import com.kompralo.model.ForumPost
import com.kompralo.model.ForumReply
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ForumReplyRepository : JpaRepository<ForumReply, Long> {
    fun findByPostOrderByCreatedAtAsc(post: ForumPost): List<ForumReply>
    fun countByPost(post: ForumPost): Long

    @Modifying
    @Query("UPDATE ForumReply r SET r.parentReply = NULL WHERE r.parentReply.id = :parentId")
    fun nullifyParentReply(parentId: Long)
}
