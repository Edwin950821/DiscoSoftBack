package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_comparativos")
data class DiscoComparativo(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val fecha: String,

    @Column(name = "total_conteo", nullable = false)
    var totalConteo: Int = 0,

    @Column(name = "total_tiquets", nullable = false)
    var totalTiquets: Int = 0,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "comparativo", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lineas: MutableList<DiscoLineaComparativo> = mutableListOf()
)

@Entity
@Table(name = "disco_linea_comparativo")
data class DiscoLineaComparativo(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(name = "producto_id", nullable = false)
    val productoId: Long,

    @Column(nullable = false)
    val nombre: String,

    @Column(nullable = false)
    val conteo: Int = 0,

    @Column(nullable = false)
    val tiquets: Int = 0,

    @Column(nullable = false)
    val diferencia: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparativo_id", nullable = false)
    var comparativo: DiscoComparativo? = null
)
