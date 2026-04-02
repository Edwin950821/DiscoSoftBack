package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "negocios")
data class Negocio(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val nombre: String,

    @Column(nullable = false, unique = true)
    val slug: String,

    @Column(name = "logo_url")
    val logoUrl: String? = null,

    @Column(name = "color_primario", nullable = false)
    val colorPrimario: String = "#D4AF37",

    @Column(nullable = false)
    var activo: Boolean = true,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now()
)
