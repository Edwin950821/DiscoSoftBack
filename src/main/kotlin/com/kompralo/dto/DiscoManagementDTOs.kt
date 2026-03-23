package com.kompralo.dto

import java.util.UUID

// ─── Producto ───

data class DiscoProductoRequest(
    val nombre: String,
    val precio: Int,
    val activo: Boolean = true
)

data class DiscoProductoResponse(
    val id: UUID,
    val nombre: String,
    val precio: Int,
    val activo: Boolean
)

data class DiscoProductoUpdateRequest(
    val nombre: String? = null,
    val precio: Int? = null,
    val activo: Boolean? = null
)

// ─── Mesero ───

data class DiscoMeseroRequest(
    val nombre: String,
    val color: String,
    val avatar: String,
    val activo: Boolean = true,
    val username: String? = null,
    val password: String? = null
)

data class DiscoMeseroResponse(
    val id: UUID,
    val nombre: String,
    val color: String,
    val avatar: String,
    val activo: Boolean,
    val username: String? = null
)

data class DiscoMeseroUpdateRequest(
    val nombre: String? = null,
    val color: String? = null,
    val avatar: String? = null,
    val activo: Boolean? = null
)

// ─── Jornada ───

data class DiscoMeseroJornadaRequest(
    val meseroId: UUID,
    val nombre: String,
    val color: String,
    val avatar: String,
    val totalMesero: Int = 0,
    val cortesias: Int = 0,
    val gastos: Int = 0,
    val pagos: Map<String, Int> = emptyMap()
)

data class DiscoMeseroJornadaResponse(
    val meseroId: UUID,
    val nombre: String,
    val color: String,
    val avatar: String,
    val totalMesero: Int,
    val cortesias: Int,
    val gastos: Int,
    val pagos: Map<String, Int>
)

data class DiscoJornadaRequest(
    val sesion: String,
    val fecha: String,
    val meseros: List<DiscoMeseroJornadaRequest>
)

data class DiscoJornadaResponse(
    val id: UUID,
    val sesion: String,
    val fecha: String,
    val meseros: List<DiscoMeseroJornadaResponse>,
    val pagos: Map<String, Int>,
    val cortesias: Int,
    val gastos: Int,
    val totalVendido: Int,
    val totalRecibido: Int,
    val saldo: Int
)

// ─── Inventario ───

data class DiscoLineaInventarioRequest(
    val productoId: UUID,
    val nombre: String,
    val valorUnitario: Int,
    val invInicial: Int = 0,
    val entradas: Int = 0,
    val invFisico: Int = 0,
    val saldo: Int = 0,
    val total: Int = 0
)

data class DiscoLineaInventarioResponse(
    val productoId: UUID,
    val nombre: String,
    val valorUnitario: Int,
    val invInicial: Int,
    val entradas: Int,
    val invFisico: Int,
    val saldo: Int,
    val total: Int
)

data class DiscoInventarioRequest(
    val fecha: String,
    val lineas: List<DiscoLineaInventarioRequest>,
    val totalGeneral: Int
)

data class DiscoInventarioResponse(
    val id: UUID,
    val fecha: String,
    val lineas: List<DiscoLineaInventarioResponse>,
    val totalGeneral: Int
)

// ─── Comparativo ───

data class DiscoLineaComparativoRequest(
    val productoId: UUID,
    val nombre: String,
    val conteo: Int = 0,
    val tiquets: Int = 0,
    val diferencia: Int = 0
)

data class DiscoLineaComparativoResponse(
    val productoId: UUID,
    val nombre: String,
    val conteo: Int,
    val tiquets: Int,
    val diferencia: Int
)

data class DiscoComparativoRequest(
    val fecha: String,
    val lineas: List<DiscoLineaComparativoRequest>,
    val totalConteo: Int,
    val totalTiquets: Int
)

data class DiscoComparativoResponse(
    val id: UUID,
    val fecha: String,
    val lineas: List<DiscoLineaComparativoResponse>,
    val totalConteo: Int,
    val totalTiquets: Int
)
