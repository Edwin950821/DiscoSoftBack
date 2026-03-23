package com.kompralo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "disco_linea_inventario")
data class DiscoLineaInventario(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false)
    val id: UUID? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventario_id", nullable = false)
    var inventario: DiscoInventario? = null,

    @Column(name = "producto_id", nullable = false, columnDefinition = "uuid")
    val productoId: UUID,

    @Column(nullable = false)
    val nombre: String,

    @Column(name = "valor_unitario", nullable = false)
    val valorUnitario: Int,

    @Column(name = "inv_inicial", nullable = false)
    var invInicial: Int = 0,

    @Column(nullable = false)
    var entradas: Int = 0,

    @Column(name = "inv_fisico", nullable = false)
    var invFisico: Int = 0,

    @Column(nullable = false)
    var saldo: Int = 0,

    @Column(nullable = false)
    var total: Int = 0,

    @Column(name = "negocio_id", nullable = false, columnDefinition = "uuid")
    val negocioId: UUID = UUID(0, 0)
)
