package com.kompralo.dto

data class NegocioConsolidado(
    val negocioId: String,
    val nombre: String,
    val slug: String,
    val colorPrimario: String,
    val totalVendido: Long,
    val totalRecibido: Long,
    val saldo: Long,
    val jornadasCount: Int
)

data class TopProducto(
    val productoId: String,
    val nombre: String,
    val cantidad: Int,
    val total: Long
)

data class TopMesero(
    val meseroId: String,
    val nombre: String,
    val color: String,
    val totalVendido: Long,
    val jornadasCount: Int
)

data class TendenciaDia(
    val fecha: String,
    val total: Long
)

data class ConsolidadoResponse(
    val totalVendido: Long,
    val totalRecibido: Long,
    val totalSaldo: Long,
    val totalCortesias: Long,
    val totalGastos: Long,
    val jornadasCount: Int,
    val negociosCount: Int,
    val porNegocio: List<NegocioConsolidado>,
    val pagosTotales: Map<String, Long> = emptyMap(),
    val totalMesActual: Long = 0,
    val totalMesAnterior: Long = 0,
    val tendencia30Dias: List<TendenciaDia> = emptyList(),
    val topProductos: List<TopProducto> = emptyList(),
    val topMeseros: List<TopMesero> = emptyList()
)
