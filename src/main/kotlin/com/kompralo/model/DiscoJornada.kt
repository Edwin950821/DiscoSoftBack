package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_jornadas")
data class DiscoJornada(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val sesion: String,

    @Column(nullable = false)
    val fecha: String,

    @Column(name = "total_vendido", nullable = false)
    var totalVendido: Int = 0,

    @Column(name = "total_recibido", nullable = false)
    var totalRecibido: Int = 0,

    @Column(nullable = false)
    var saldo: Int = 0,

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

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "negocio_id", nullable = false, columnDefinition = "uuid")
    val negocioId: UUID = UUID(0, 0),

    @OneToMany(mappedBy = "jornada", cascade = [CascadeType.ALL], orphanRemoval = true)
    val meseros: MutableList<DiscoMeseroJornada> = mutableListOf()
)
