package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_jornada_diaria", uniqueConstraints = [UniqueConstraint(name = "uq_jornada_diaria_fecha_negocio", columnNames = ["fecha", "negocio_id"])])
data class DiscoJornadaDiaria(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val fecha: String,

    @Column(name = "total_ventas", nullable = false)
    val totalVentas: Int = 0,

    @Column(name = "total_billar", nullable = false)
    val totalBillar: Int = 0,

    @Column(name = "total_general", nullable = false)
    val totalGeneral: Int = 0,

    @Column(name = "cuentas_cerradas", nullable = false)
    val cuentasCerradas: Int = 0,

    @Column(name = "tickets_totales", nullable = false)
    val ticketsTotales: Int = 0,

    @Column(name = "mesas_atendidas", nullable = false)
    val mesasAtendidas: Int = 0,

    @Column(name = "partidas_billar", nullable = false)
    val partidasBillar: Int = 0,

    @Column(name = "cerrado_en", nullable = false, updatable = false)
    val cerradoEn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "negocio_id", nullable = false, columnDefinition = "uuid")
    val negocioId: UUID = UUID(0, 0)
)
