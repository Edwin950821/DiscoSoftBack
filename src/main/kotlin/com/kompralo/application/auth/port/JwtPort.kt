package com.kompralo.application.auth.port

import com.kompralo.domain.auth.valueobject.Email
import com.kompralo.domain.auth.valueobject.JwtToken

interface JwtPort {
    fun generateToken(email: Email): JwtToken
    fun validateToken(token: JwtToken): Boolean
    fun extractEmail(token: JwtToken): Email?
}
