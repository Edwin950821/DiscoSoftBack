package com.kompralo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "disco_linea_pedido")
data class DiscoLineaPedido(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    var pedido: DiscoPedido? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    val producto: DiscoProducto,

    @Column(nullable = false)
    val nombre: String,

    @Column(name = "precio_unitario", nullable = false)
    val precioUnitario: Int,

    @Column(nullable = false)
    val cantidad: Int = 1,

    @Column(nullable = false)
    val total: Int = 0
)
