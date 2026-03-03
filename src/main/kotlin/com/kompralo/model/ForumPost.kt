package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class ForumCategory {
    GENERAL,
    ESTRATEGIAS,
    NUEVAS_FUNCIONES,
    AYUDA_TECNICA,
    TENDENCIAS
}

@Entity
@Table(name = "forum_posts")
class ForumPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: ForumCategory = ForumCategory.GENERAL,

    @Column(nullable = false)
    var viewCount: Long = 0,

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("createdAt ASC")
    val replies: MutableList<ForumReply> = mutableListOf(),

    @OneToMany(mappedBy = "post", cascade = [CascadeType.ALL], orphanRemoval = true)
    val likes: MutableList<ForumLike> = mutableListOf(),

    @Column(columnDefinition = "TEXT")
    var imageUrls: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ForumPost) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
