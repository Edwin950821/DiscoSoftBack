package com.kompralo.dto

data class TwoFactorStatusResponse(
    val enabled: Boolean,
    val backupCodesRemaining: Int?
)
