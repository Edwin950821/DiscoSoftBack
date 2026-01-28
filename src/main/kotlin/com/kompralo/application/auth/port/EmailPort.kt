package com.kompralo.application.auth.port

interface EmailPort {
    fun sendPasswordResetEmail(
        to: String,
        userName: String,
        resetToken: String,
        resetUrl: String
    ): Boolean

    fun sendWelcomeEmail(to: String, userName: String): Boolean

    fun sendSellerVerifiedEmail(to: String, businessName: String): Boolean
}
