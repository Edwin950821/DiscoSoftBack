package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_mesas_billar")
data class DiscoMesaBillar(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val numero: Int,

    @Column(nullable = false)
    var nombre: String,

    @Column(name = "precio_por_hora", nullable = false)
    var precioPorHora: Int = 20000,

    @Column(nullable = false)
    var estado: String = "LIBRE",

    @Column(nullable = false)
    var activo: Boolean = true,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now()
)
