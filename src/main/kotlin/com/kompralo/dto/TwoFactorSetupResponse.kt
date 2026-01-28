package com.kompralo.dto

/**
 * DTO para respuesta de setup de 2FA
 * Contiene el secreto, QR code URL y códigos de respaldo
 */
data class TwoFactorSetupResponse(
    val secret: String,
    val qrCodeUrl: String,
    val backupCodes: List<String>
)
