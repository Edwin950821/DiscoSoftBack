package com.kompralo.repository

import com.kompralo.model.ForumCategory
import com.kompralo.model.ForumPost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ForumPostRepository : JpaRepository<ForumPost, Long> {

    @Query("SELECT p FROM ForumPost p ORDER BY p.createdAt DESC")
    fun findAllPosts(pageable: Pageable): Page<ForumPost>

    @Query("SELECT p FROM ForumPost p WHERE p.category = :category ORDER BY p.createdAt DESC")
    fun findByCategory(category: ForumCategory, pageable: Pageable): Page<ForumPost>

    @Query("SELECT p FROM ForumPost p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY p.createdAt DESC")
    fun findBySearch(search: String, pageable: Pageable): Page<ForumPost>

    @Query("SELECT p FROM ForumPost p WHERE p.category = :category AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY p.createdAt DESC")
    fun findByCategoryAndSearch(category: ForumCategory, search: String, pageable: Pageable): Page<ForumPost>

    @Modifying
    @Query("UPDATE ForumPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    fun incrementViewCount(id: Long)
}
