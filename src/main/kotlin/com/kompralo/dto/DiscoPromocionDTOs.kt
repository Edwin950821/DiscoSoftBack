package com.kompralo.dto

import java.util.UUID

data class DiscoPromocionRequest(
    val nombre: String,
    val compraProductoIds: List<UUID>,
    val compraCantidad: Int,
    val regaloProductoId: UUID,
    val regaloCantidad: Int = 1
)

data class DiscoPromocionUpdateRequest(
    val nombre: String? = null,
    val compraProductoIds: List<UUID>? = null,
    val compraCantidad: Int? = null,
    val regaloProductoId: UUID? = null,
    val regaloCantidad: Int? = null,
    val activa: Boolean? = null
)

data class DiscoPromocionResponse(
    val id: UUID,
    val nombre: String,
    val compraProductoIds: List<UUID>,
    val compraProductoNombres: List<String>,
    val compraCantidad: Int,
    val regaloProductoId: UUID,
    val regaloProductoNombre: String,
    val regaloProductoPrecio: Int,
    val regaloCantidad: Int,
    val activa: Boolean
)

data class PromoAplicableResponse(
    val promoId: UUID,
    val promoNombre: String,
    val compraProductoNombres: List<String>,
    val compraCantidad: Int,
    val regaloProductoNombre: String,
    val regaloProductoPrecio: Int,
    val regaloCantidad: Int,
    val cantidadCompraEnCuenta: Int,
    val setsCalificados: Int,
    val unidadesRegaloCalificadas: Int,
    val unidadesRegaloEnCuenta: Int,
    val unidadesRegaloCortesia: Int,
    val descuento: Int
)

data class PagarCuentaRequest(
    val promoIds: List<UUID> = emptyList()
)
