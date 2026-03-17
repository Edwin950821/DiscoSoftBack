package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_partidas_billar")
data class DiscoPartidaBillar(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_billar_id", nullable = false)
    val mesaBillar: DiscoMesaBillar,

    @Column(name = "nombre_cliente")
    val nombreCliente: String = "Cliente",

    @Column(name = "hora_inicio", nullable = false)
    val horaInicio: LocalDateTime = LocalDateTime.now(),

    @Column(name = "hora_fin")
    var horaFin: LocalDateTime? = null,

    @Column(name = "precio_por_hora", nullable = false)
    val precioPorHora: Int,

    @Column(name = "horas_cobradas")
    var horasCobradas: Int? = null,

    @Column
    var total: Int? = null,

    @Column(nullable = false)
    var estado: String = "EN_JUEGO",

    @Column(name = "jornada_fecha", nullable = false)
    val jornadaFecha: String,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now()
)
