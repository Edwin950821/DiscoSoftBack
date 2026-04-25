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

data class ConsolidadoResponse(
    val totalVendido: Long,
    val totalRecibido: Long,
    val totalSaldo: Long,
    val totalCortesias: Long,
    val totalGastos: Long,
    val jornadasCount: Int,
    val negociosCount: Int,
    val porNegocio: List<NegocioConsolidado>
)
