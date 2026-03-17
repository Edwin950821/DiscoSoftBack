package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_cuenta_mesa")
data class DiscoCuentaMesa(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = false)
    val mesa: DiscoMesa,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id", nullable = false)
    val mesero: DiscoMesero,

    @Column(name = "nombre_cliente", nullable = false)
    val nombreCliente: String = "Cliente",

    @Column(name = "jornada_fecha", nullable = false)
    val jornadaFecha: String,

    @Column(nullable = false)
    var total: Int = 0,

    @Column(nullable = false)
    var estado: String = "ABIERTA",

    @Column(name = "descuento_promo", nullable = false)
    var descuentoPromo: Int = 0,

    @Column(name = "pagada_en")
    var pagadaEn: LocalDateTime? = null,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now()
)
