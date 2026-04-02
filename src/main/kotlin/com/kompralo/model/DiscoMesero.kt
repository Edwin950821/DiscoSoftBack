package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_meseros", uniqueConstraints = [UniqueConstraint(name = "uq_mesero_username_negocio", columnNames = ["username", "negocio_id"])])
data class DiscoMesero(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    var nombre: String,

    @Column(nullable = false)
    var color: String,

    @Column(nullable = false)
    var avatar: String,

    @Column(nullable = false)
    var activo: Boolean = true,

    @Column
    val username: String? = null,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "negocio_id", nullable = false, columnDefinition = "uuid")
    val negocioId: UUID = UUID(0, 0)
)
