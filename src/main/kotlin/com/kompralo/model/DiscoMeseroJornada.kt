package com.kompralo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "disco_mesero_jornada")
data class DiscoMeseroJornada(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jornada_id", nullable = false)
    var jornada: DiscoJornada? = null,

    @Column(name = "mesero_id", nullable = false, columnDefinition = "uuid")
    val meseroId: UUID,

    @Column(nullable = false)
    val nombre: String,

    @Column(nullable = false)
    val color: String,

    @Column(nullable = false)
    val avatar: String,

    @Column(name = "total_mesero", nullable = false)
    var totalMesero: Int = 0,

    @Column(nullable = false)
    var cortesias: Int = 0,

    @Column(nullable = false)
    var gastos: Int = 0,

    @Column(name = "pagos_efectivo", nullable = false)
    var pagosEfectivo: Int = 0,

    @Column(name = "pagos_qr", nullable = false)
    var pagosQR: Int = 0,

    @Column(name = "pagos_nequi", nullable = false)
    var pagosNequi: Int = 0,

    @Column(name = "pagos_datafono", nullable = false)
    var pagosDatafono: Int = 0,

    @Column(name = "pagos_vales", nullable = false)
    var pagosVales: Int = 0,

    @Column(name = "transacciones_detalle", columnDefinition = "TEXT")
    var transaccionesDetalle: String? = null,

    @Column(name = "vales_detalle", columnDefinition = "TEXT")
    var valesDetalle: String? = null,

    @Column(name = "cortesias_detalle", columnDefinition = "TEXT")
    var cortesiasDetalle: String? = null,

    @Column(name = "gastos_detalle", columnDefinition = "TEXT")
    var gastosDetalle: String? = null,

    @Column(name = "lineas_detalle", columnDefinition = "TEXT")
    var lineasDetalle: String? = null
)
