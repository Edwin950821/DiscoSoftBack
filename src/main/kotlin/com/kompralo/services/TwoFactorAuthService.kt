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

/**
 * Servicio para autenticación de dos factores (2FA) usando TOTP
 *
 * Implementa el algoritmo TOTP (RFC 6238) compatible con Google Authenticator
 */
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

    /**
     * Inicia el proceso de setup de 2FA para un usuario
     * Genera un secreto TOTP y códigos de respaldo
     *
     * @param user Usuario para configurar 2FA
     * @return Respuesta con secreto, QR code URL y códigos de respaldo
     */
    @Transactional
    fun setupTwoFactor(user: User): TwoFactorSetupResponse {
        // Generar secreto TOTP
        val secret = generateSecretKey()

        // Generar códigos de respaldo
        val backupCodes = generateBackupCodes(10)

        // Guardar o actualizar configuración 2FA (pero no habilitada aún)
        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .map { existing ->
                // Actualizar secreto existente
                TwoFactorAuth(
                    id = existing.id,
                    user = user,
                    secret = secret,
                    enabled = false, // No habilitar hasta verificar
                    backupCodes = backupCodes.toTypedArray(),
                    createdAt = existing.createdAt
                )
            }
            .orElse(
                // Crear nueva configuración
                TwoFactorAuth(
                    user = user,
                    secret = secret,
                    enabled = false,
                    backupCodes = backupCodes.toTypedArray()
                )
            )

        twoFactorAuthRepository.save(twoFactorAuth)

        // Generar URL de QR code
        val qrCodeUrl = generateQRCodeUrl(user.email, secret)

        logger.info("2FA setup iniciado para usuario: ${user.email}")

        return TwoFactorSetupResponse(
            secret = secret,
            qrCodeUrl = qrCodeUrl,
            backupCodes = backupCodes
        )
    }

    /**
     * Habilita 2FA después de verificar el código
     *
     * @param user Usuario que habilita 2FA
     * @param code Código TOTP de 6 dígitos
     * @throws IllegalArgumentException si el código es inválido
     */
    @Transactional
    fun enableTwoFactor(user: User, code: String) {
        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("No se ha iniciado el setup de 2FA") }

        // Verificar código
        if (!verifyCode(twoFactorAuth.secret, code)) {
            throw IllegalArgumentException("Código inválido")
        }

        // Habilitar 2FA
        twoFactorAuth.enabled = true
        twoFactorAuthRepository.save(twoFactorAuth)

        logger.info("2FA habilitado para usuario: ${user.email}")
    }

    /**
     * Deshabilita 2FA para un usuario
     *
     * @param user Usuario que deshabilita 2FA
     * @param code Código TOTP de 6 dígitos para confirmar
     * @throws IllegalArgumentException si el código es inválido
     */
    @Transactional
    fun disableTwoFactor(user: User, code: String) {
        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("2FA no está configurado") }

        // Verificar código o código de respaldo
        val isValid = verifyCode(twoFactorAuth.secret, code) || twoFactorAuth.isValidBackupCode(code)

        if (!isValid) {
            throw IllegalArgumentException("Código inválido")
        }

        // Si usó código de respaldo, consumirlo
        if (twoFactorAuth.isValidBackupCode(code)) {
            twoFactorAuth.useBackupCode(code)
        }

        // Deshabilitar 2FA
        twoFactorAuth.enabled = false
        twoFactorAuthRepository.save(twoFactorAuth)

        logger.info("2FA deshabilitado para usuario: ${user.email}")
    }

    /**
     * Verifica un código 2FA (TOTP o código de respaldo)
     *
     * @param user Usuario a verificar
     * @param code Código de 6 dígitos
     * @return true si el código es válido
     */
    @Transactional
    fun verifyTwoFactorCode(user: User, code: String): Boolean {
        val twoFactorAuth = twoFactorAuthRepository.findByUserAndEnabledTrue(user)
            .orElse(null) ?: return false

        // Primero intentar con TOTP
        if (verifyCode(twoFactorAuth.secret, code)) {
            return true
        }

        // Si falla, intentar con código de respaldo
        if (twoFactorAuth.isValidBackupCode(code)) {
            // Consumir código de respaldo (son de un solo uso)
            twoFactorAuth.useBackupCode(code)
            twoFactorAuthRepository.save(twoFactorAuth)
            logger.info("Código de respaldo usado para usuario: ${user.email}")
            return true
        }

        return false
    }

    /**
     * Obtiene el estado de 2FA de un usuario
     *
     * @param user Usuario
     * @return Estado de 2FA
     */
    fun getTwoFactorStatus(user: User): TwoFactorStatusResponse {
        val twoFactorAuth = twoFactorAuthRepository.findByUser(user)
            .orElse(null)

        return TwoFactorStatusResponse(
            enabled = twoFactorAuth?.enabled ?: false,
            backupCodesRemaining = twoFactorAuth?.backupCodes?.size
        )
    }

    /**
     * Verifica si un usuario tiene 2FA habilitado
     */
    fun isTwoFactorEnabled(user: User): Boolean {
        return twoFactorAuthRepository.findByUserAndEnabledTrue(user).isPresent
    }

    // ==================== MÉTODOS PRIVADOS TOTP ====================

    /**
     * Genera un secreto TOTP aleatorio de 160 bits (20 bytes) en Base32
     */
    private fun generateSecretKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20) // 160 bits
        random.nextBytes(bytes)
        return base32.encodeToString(bytes).replace("=", "")
    }

    /**
     * Genera códigos de respaldo aleatorios
     *
     * @param count Número de códigos a generar
     * @return Lista de códigos de 8 caracteres
     */
    private fun generateBackupCodes(count: Int): List<String> {
        val random = SecureRandom()
        return (1..count).map {
            val code = random.nextInt(100000000).toString().padStart(8, '0')
            code
        }
    }

    /**
     * Genera la URL para el QR code de Google Authenticator
     *
     * Formato: otpauth://totp/ISSUER:EMAIL?secret=SECRET&issuer=ISSUER
     */
    private fun generateQRCodeUrl(email: String, secret: String): String {
        val issuer = URLEncoder.encode(appName, StandardCharsets.UTF_8)
        val encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8)

        return "otpauth://totp/$issuer:$encodedEmail?secret=$secret&issuer=$issuer"
    }

    /**
     * Verifica un código TOTP contra el secreto
     *
     * @param secret Secreto en Base32
     * @param code Código de 6 dígitos a verificar
     * @return true si el código es válido
     */
    private fun verifyCode(secret: String, code: String): Boolean {
        try {
            // Obtener timestamp actual (en intervalos de 30 segundos)
            val currentTimeSlot = System.currentTimeMillis() / 1000 / 30

            // Verificar código actual y ±1 ventana de tiempo (para compensar desincronización)
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

    /**
     * Genera un código TOTP de 6 dígitos usando HMAC-SHA1
     *
     * @param secretBase32 Secreto codificado en Base32
     * @param timeSlot Slot de tiempo (timestamp / 30)
     * @return Código de 6 dígitos
     */
    private fun generateTOTP(secretBase32: String, timeSlot: Long): String {
        // Decodificar secreto de Base32
        val key = base32.decode(secretBase32)

        // Convertir timestamp a bytes (big-endian)
        val data = ByteArray(8)
        var value = timeSlot
        for (i in 7 downTo 0) {
            data[i] = value.toByte()
            value = value shr 8
        }

        // Calcular HMAC-SHA1
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)

        // Extraer offset dinámico (últimos 4 bits del hash)
        val offset = (hash[hash.size - 1].toInt() and 0x0F)

        // Extraer 4 bytes desde el offset
        val truncatedHash = (
            ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        )

        // Aplicar módulo para obtener código de N dígitos
        val code = truncatedHash % (10.0.pow(codeLength.toDouble()).toInt())

        // Formatear con ceros a la izquierda
        return code.toString().padStart(codeLength, '0')
    }
}
