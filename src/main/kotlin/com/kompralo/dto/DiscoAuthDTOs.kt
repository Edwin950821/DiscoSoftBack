package com.kompralo.dto

enum class DiscoRol { ADMINISTRADOR, DUENO, MESERO, SUPER }

data class DiscoLoginRequest(
    val username: String,
    val password: String,
    val rol: DiscoRol = DiscoRol.ADMINISTRADOR
)

data class NegocioInfo(
    val id: String,
    val nombre: String,
    val slug: String,
    val colorPrimario: String,
    val logoUrl: String? = null
)

data class DiscoAuthResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val nombre: String,
    val rol: DiscoRol,
    val meseroId: String? = null,
    val negocioId: String? = null,
    val negocioNombre: String? = null,
    val negocios: List<NegocioInfo> = emptyList(),
    val mensaje: String = "Bienvenido a Monastery Club"
)
