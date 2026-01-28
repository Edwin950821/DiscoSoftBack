package com.kompralo.dto

/**
 * DTO para estado de 2FA del usuario
 */
data class TwoFactorStatusResponse(
    val enabled: Boolean,
    val backupCodesRemaining: Int?
)
