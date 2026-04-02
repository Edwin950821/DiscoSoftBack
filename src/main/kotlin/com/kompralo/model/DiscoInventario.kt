package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_inventarios")
data class DiscoInventario(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val fecha: String,

    @Column(name = "total_general", nullable = false)
    var totalGeneral: Int = 0,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "negocio_id", nullable = false, columnDefinition = "uuid")
    val negocioId: UUID = UUID(0, 0),

    @OneToMany(mappedBy = "inventario", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lineas: MutableList<DiscoLineaInventario> = mutableListOf()
)
