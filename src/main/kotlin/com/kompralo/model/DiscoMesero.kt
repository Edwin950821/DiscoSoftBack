package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_meseros")
data class DiscoMesero(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val nombre: String,

    @Column(nullable = false)
    val color: String,

    @Column(nullable = false)
    val avatar: String,

    @Column(nullable = false)
    var activo: Boolean = true,

    @Column(unique = true)
    val username: String? = null,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now()
)
