package com.kompralo.dto

data class TwoFactorSetupResponse(
    val secret: String,
    val qrCodeUrl: String,
    val backupCodes: List<String>
)
