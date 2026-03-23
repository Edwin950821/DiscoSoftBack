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
    @JoinColumn(name = "mesero_id")
    var mesero: DiscoMesero?,

    @Column(name = "ticket_dia", nullable = false)
    val ticketDia: Int,

    @Column(nullable = false)
    var estado: String = "PENDIENTE",

    @Column(nullable = false)
    var total: Int = 0,

    @Column(name = "jornada_fecha", nullable = false)
    val jornadaFecha: String,

    val nota: String? = null,

    @Column(name = "es_cortesia", nullable = false)
    val esCortesia: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_id")
    val promo: DiscoPromocion? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id")
    var cuenta: DiscoCuentaMesa? = null,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "despachado_en")
    var despachadoEn: LocalDateTime? = null,

    @Column(name = "cancelado_en")
    var canceladoEn: LocalDateTime? = null,

    @Column(name = "negocio_id", nullable = false, columnDefinition = "uuid")
    val negocioId: UUID = UUID(0, 0),

    @OneToMany(mappedBy = "pedido", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lineas: MutableList<DiscoLineaPedido> = mutableListOf()
)
