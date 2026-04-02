package com.kompralo.dto

enum class DiscoRol { ADMINISTRADOR, DUENO, MESERO }

data class DiscoLoginRequest(
    val username: String,
    val password: String,
    val rol: DiscoRol = DiscoRol.ADMINISTRADOR
)

data class DiscoAuthResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val nombre: String,
    val rol: DiscoRol,
    val meseroId: String? = null,
    val negocioId: String? = null,
    val negocioNombre: String? = null,
    val mensaje: String = "Bienvenido a Monastery Club"
)
