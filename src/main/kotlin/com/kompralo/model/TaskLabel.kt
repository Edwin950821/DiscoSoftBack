package com.kompralo.model

import jakarta.persistence.*

@Entity
@Table(name = "task_labels")
data class TaskLabel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String,

    @Column(nullable = false, length = 7)
    var color: String = "#6366f1",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    val createdBy: User
)
