package com.kompralo.domain.auth.model

import com.kompralo.domain.auth.valueobject.TotpSecret
import java.time.LocalDateTime

data class TwoFactorAuth(
    val id: Long?,
    val userId: Long,
    val secret: TotpSecret,
    val enabled: Boolean,
    val backupCodes: List<String>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun enable(): TwoFactorAuth {
        return copy(enabled = true, updatedAt = LocalDateTime.now())
    }

    fun disable(): TwoFactorAuth {
        return copy(enabled = false, updatedAt = LocalDateTime.now())
    }

    fun isValidBackupCode(code: String): Boolean {
        return backupCodes.contains(code)
    }

    fun useBackupCode(code: String): TwoFactorAuth {
        require(isValidBackupCode(code)) { "Código de respaldo inválido" }
        val updatedCodes = backupCodes.filter { it != code }
        return copy(backupCodes = updatedCodes, updatedAt = LocalDateTime.now())
    }

    fun hasBackupCodesRemaining(): Boolean = backupCodes.isNotEmpty()

    fun backupCodesCount(): Int = backupCodes.size
}
