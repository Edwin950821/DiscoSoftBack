package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_productos")
data class DiscoProducto(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    var nombre: String,

    @Column(nullable = false)
    var precio: Int,

    @Column(nullable = false)
    var activo: Boolean = true,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "negocio_id", nullable = true, columnDefinition = "uuid")
    val negocioId: UUID = UUID(0, 0)
)
