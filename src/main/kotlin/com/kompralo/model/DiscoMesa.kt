package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_mesas")
data class DiscoMesa(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val numero: Int,

    @Column(nullable = false)
    val nombre: String,

    @Column(nullable = false)
    var estado: String = "LIBRE",

    @Column(name = "nombre_cliente")
    var nombreCliente: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id")
    var mesero: DiscoMesero? = null,

    @Column(name = "jornada_id", columnDefinition = "uuid")
    var jornadaId: UUID? = null,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now()
)
