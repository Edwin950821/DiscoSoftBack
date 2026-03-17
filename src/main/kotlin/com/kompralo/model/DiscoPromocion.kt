package com.kompralo.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "disco_promociones")
data class DiscoPromocion(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val nombre: String,

    @Column(name = "compra_producto_ids", nullable = false)
    val compraProductoIds: String,

    @Column(name = "compra_cantidad", nullable = false)
    val compraCantidad: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regalo_producto_id", nullable = false)
    val regaloProducto: DiscoProducto,

    @Column(name = "regalo_cantidad", nullable = false)
    val regaloCantidad: Int = 1,

    @Column(nullable = false)
    var activa: Boolean = false,

    @Column(name = "creado_en", nullable = false, updatable = false)
    val creadoEn: LocalDateTime = LocalDateTime.now()
)
