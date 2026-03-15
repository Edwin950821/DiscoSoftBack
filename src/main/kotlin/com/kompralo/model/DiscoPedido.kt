package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_pedidos")
data class DiscoPedido(
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

    @Column(name = "ticket_dia", nullable = false)
    val ticketDia: Int,

    @Column(nullable = false)
    var estado: String = "PENDIENTE",

    @Column(nullable = false)
    var total: Int = 0,

    @Column(name = "jornada_fecha", nullable = false)
    val jornadaFecha: String,

    val nota: String? = null,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "despachado_en")
    var despachadoEn: LocalDateTime? = null,

    @Column(name = "cancelado_en")
    var canceladoEn: LocalDateTime? = null,

    @OneToMany(mappedBy = "pedido", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lineas: MutableList<DiscoLineaPedido> = mutableListOf()
)
