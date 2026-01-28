package com.kompralo.application.auth.port

data class GoogleUserInfo(
    val email: String,
    val name: String,
    val googleUserId: String
)

interface GoogleAuthPort {
    fun verifyToken(credential: String): GoogleUserInfo?
}
