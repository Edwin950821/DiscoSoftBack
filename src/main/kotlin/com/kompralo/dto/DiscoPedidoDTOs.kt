package com.kompralo.dto

import java.util.UUID

data class DiscoMesaRequest(
    val numero: Int,
    val nombre: String
)

data class DiscoAtenderMesaRequest(
    val meseroId: UUID,
    val nombreCliente: String
)

data class DiscoMesaResponse(
    val id: UUID,
    val numero: Int,
    val nombre: String,
    val estado: String,
    val nombreCliente: String? = null,
    val meseroId: UUID? = null,
    val meseroNombre: String? = null,
    val meseroColor: String? = null,
    val meseroAvatar: String? = null
)

data class DiscoLineaPedidoRequest(
    val productoId: UUID,
    val cantidad: Int
)

data class DiscoLineaPedidoResponse(
    val id: UUID,
    val productoId: UUID,
    val nombre: String,
    val precioUnitario: Int,
    val cantidad: Int,
    val total: Int
)

data class DiscoPedidoRequest(
    val mesaId: UUID,
    val meseroId: UUID,
    val lineas: List<DiscoLineaPedidoRequest>,
    val nota: String? = null
)

data class DiscoPedidoResponse(
    val id: UUID,
    val mesaId: UUID,
    val mesaNumero: Int,
    val mesaNombre: String,
    val meseroId: UUID,
    val meseroNombre: String,
    val meseroColor: String,
    val meseroAvatar: String,
    val ticketDia: Int,
    val estado: String,
    val total: Int,
    val jornadaFecha: String,
    val nota: String?,
    val esCortesia: Boolean = false,
    val promoNombre: String? = null,
    val lineas: List<DiscoLineaPedidoResponse>,
    val creadoEn: String,
    val despachadoEn: String? = null
)

data class DiscoCuentaMesaResponse(
    val id: UUID,
    val mesaId: UUID,
    val mesaNumero: Int,
    val mesaNombre: String,
    val nombreCliente: String,
    val meseroId: UUID,
    val meseroNombre: String,
    val meseroColor: String,
    val meseroAvatar: String,
    val jornadaFecha: String,
    val total: Int,
    val descuentoPromo: Int = 0,
    val totalConDescuento: Int? = null,
    val estado: String,
    val pedidos: List<DiscoPedidoResponse>,
    val creadoEn: String
)

data class DiscoResumenDiaResponse(
    val fecha: String,
    val totalVentas: Int,
    val totalBillar: Int,
    val totalGeneral: Int,
    val cuentasCerradas: Int,
    val cuentasAbiertas: Int,
    val ticketsTotales: Int,
    val mesasAtendidas: Int,
    val partidasBillar: Int,
    val jornadaCerrada: Boolean
)

data class DiscoResumenJornadaResponse(
    val id: UUID,
    val fecha: String,
    val totalVentas: Int,
    val totalBillar: Int,
    val totalGeneral: Int,
    val cuentasCerradas: Int,
    val ticketsTotales: Int,
    val mesasAtendidas: Int,
    val partidasBillar: Int,
    val cerradoEn: String
)
