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
    // Totales por medio de pago sumando TODOS los negocios
    // Keys: "Efectivo", "QR", "Nequi", "Datafono", "Vales"
    val pagosTotales: Map<String, Long> = emptyMap(),
    // Comparativo por mes calendario
    val totalMesActual: Long = 0,
    val totalMesAnterior: Long = 0,
    // Tendencia ultimos 30 dias (fecha YYYY-MM-DD ordenada asc)
    val tendencia30Dias: List<TendenciaDia> = emptyList(),
    // Top 5 productos vendidos sumando todos los negocios
    val topProductos: List<TopProducto> = emptyList(),
    // Top 5 meseros sumando todos los negocios
    val topMeseros: List<TopMesero> = emptyList()
)
