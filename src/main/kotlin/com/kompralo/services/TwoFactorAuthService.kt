package com.kompralo.services

import com.kompralo.dto.TwoFactorSetupResponse
import com.kompralo.dto.TwoFactorStatusResponse
import com.kompralo.model.TwoFactorAuth
import com.kompralo.model.User
import com.kompralo.repository.TwoFactorAuthRepository
import org.apache.commons.codec.binary.Base32
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

@Service
class TwoFactorAuthService(
    private val twoFactorAuthRepository: TwoFactorAuthRepository
) {

    private val logger = LoggerFactory.getLogger(TwoFactorAuthService::class.java)
    private val base32 = Base32()

    @Value("\${two-factor.code-length:6}")
    private var codeLength: Int = 6

    @Value("\${spring.application.name:Kompralo}")
    private lateinit var appName: String

    @Transactional
    fun setupTwoFactor(user: User): TwoFactorSetupResponse {
        val secret = generateSecretKey()
        val backupCodes = generateBackupCodes(10)

        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .map { existing ->
                TwoFactorAuth(
                    id = existing.id,
                    user = user,
                    secret = secret,
                    enabled = false,
                    backupCodes = backupCodes.toTypedArray(),
                    createdAt = existing.createdAt
                )
            }
            .orElse(
                TwoFactorAuth(
                    user = user,
                    secret = secret,
                    enabled = false,
                    backupCodes = backupCodes.toTypedArray()
                )
            )

        twoFactorAuthRepository.save(twoFactorAuth)

        val qrCodeUrl = generateQRCodeUrl(user.email, secret)

        logger.info("2FA setup iniciado para usuario: ${user.email}")

        return TwoFactorSetupResponse(
            secret = secret,
            qrCodeUrl = qrCodeUrl,
            backupCodes = backupCodes
        )
    }

    @Transactional
    fun enableTwoFactor(user: User, code: String) {
        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("No se ha iniciado el setup de 2FA") }

        if (!verifyCode(twoFactorAuth.secret, code)) {
            throw IllegalArgumentException("Código inválido")
        }

        twoFactorAuth.enabled = true
        twoFactorAuthRepository.save(twoFactorAuth)

        logger.info("2FA habilitado para usuario: ${user.email}")
    }

    @Transactional
    fun disableTwoFactor(user: User, code: String) {
        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("2FA no está configurado") }

        val isValid = verifyCode(twoFactorAuth.secret, code) || twoFactorAuth.isValidBackupCode(code)

        if (!isValid) {
            throw IllegalArgumentException("Código inválido")
        }

        if (twoFactorAuth.isValidBackupCode(code)) {
            twoFactorAuth.useBackupCode(code)
        }

        twoFactorAuth.enabled = false
        twoFactorAuthRepository.save(twoFactorAuth)

        logger.info("2FA deshabilitado para usuario: ${user.email}")
    }

    @Transactional
    fun verifyTwoFactorCode(user: User, code: String): Boolean {
        val twoFactorAuth = twoFactorAuthRepository.findByUserAndEnabledTrue(user)
            .orElse(null) ?: return false

        if (verifyCode(twoFactorAuth.secret, code)) {
            return true
        }

        if (twoFactorAuth.isValidBackupCode(code)) {
            twoFactorAuth.useBackupCode(code)
            twoFactorAuthRepository.save(twoFactorAuth)
            logger.info("Código de respaldo usado para usuario: ${user.email}")
            return true
        }

        return false
    }

    fun getTwoFactorStatus(user: User): TwoFactorStatusResponse {
        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElse(null)

        return TwoFactorStatusResponse(
            enabled = twoFactorAuth?.enabled ?: false,
            backupCodesRemaining = twoFactorAuth?.backupCodes?.size
        )
    }

    fun isTwoFactorEnabled(user: User): Boolean {
        return twoFactorAuthRepository.findByUserAndEnabledTrue(user).isPresent
    }

    private fun generateSecretKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return base32.encodeToString(bytes).replace("=", "")
    }

    private fun generateBackupCodes(count: Int): List<String> {
        val random = SecureRandom()
        return (1..count).map {
            val code = random.nextInt(100000000).toString().padStart(8, '0')
            code
        }
    }

    private fun generateQRCodeUrl(email: String, secret: String): String {
        val issuer = URLEncoder.encode(appName, StandardCharsets.UTF_8)
        val encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8)

        return "otpauth://totp/$issuer:$encodedEmail?secret=$secret&issuer=$issuer"
    }

    private fun verifyCode(secret: String, code: String): Boolean {
        try {
            val currentTimeSlot = System.currentTimeMillis() / 1000 / 30

            for (i in -1..1) {
                val timeSlot = currentTimeSlot + i
                val generatedCode = generateTOTP(secret, timeSlot)

                if (code == generatedCode) {
                    return true
                }
            }

            return false
        } catch (e: Exception) {
            logger.error("Error al verificar código TOTP: ${e.message}", e)
            return false
        }
    }

    private fun generateTOTP(secretBase32: String, timeSlot: Long): String {
        val key = base32.decode(secretBase32)

        val data = ByteArray(8)
        var value = timeSlot
        for (i in 7 downTo 0) {
            data[i] = value.toByte()
            value = value shr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)

        val offset = (hash[hash.size - 1].toInt() and 0x0F)

        val truncatedHash = (
            ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        )

        val code = truncatedHash % (10.0.pow(codeLength.toDouble()).toInt())

        return code.toString().padStart(codeLength, '0')
    }
}
