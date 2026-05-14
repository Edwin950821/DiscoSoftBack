package com.kompralo.dto

import java.util.UUID

data class DiscoMesaBillarRequest(
    val nombre: String,
    val precioPorHora: Int = 20000
)

data class DiscoMesaBillarUpdateRequest(
    val nombre: String? = null,
    val precioPorHora: Int? = null,
    val activo: Boolean? = null
)

data class DiscoMesaBillarResponse(
    val id: UUID,
    val numero: Int,
    val nombre: String,
    val precioPorHora: Int,
    val estado: String,
    val activo: Boolean,
    val partidaActiva: DiscoPartidaBillarResponse? = null
)

data class DiscoIniciarPartidaRequest(
    val nombreCliente: String = "Cliente",
    val precioPorHora: Int? = null
)

data class DiscoTrasladarPartidaRequest(
    val mesaDestinoId: UUID
)

data class DiscoEditarPartidaRequest(
    val nombreCliente: String? = null,
    val horasCobradas: Int? = null,
    val total: Int? = null
)

data class DiscoPartidaBillarResponse(
    val id: UUID,
    val mesaBillarId: UUID,
    val mesaBillarNumero: Int,
    val mesaBillarNombre: String,
    val nombreCliente: String,
    val horaInicio: String,
    val horaFin: String? = null,
    val precioPorHora: Int,
    val horasCobradas: Int? = null,
    val total: Int? = null,
    val estado: String,
    val jornadaFecha: String,
    val creadoEn: String
)
