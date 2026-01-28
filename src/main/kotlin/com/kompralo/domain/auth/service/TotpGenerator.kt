package com.kompralo.domain.auth.service

import com.kompralo.domain.auth.valueobject.TotpSecret
import org.apache.commons.codec.binary.Base32
import org.springframework.stereotype.Service
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

@Service
class TotpGenerator {

    private val base32 = Base32()
    private val codeLength = 6

    fun generateSecret(): TotpSecret {
        val random = SecureRandom()
        val bytes = ByteArray(20) // 160 bits
        random.nextBytes(bytes)
        val secret = base32.encodeToString(bytes).replace("=", "")
        return TotpSecret(secret)
    }

    fun generateBackupCodes(count: Int = 10): List<String> {
        val random = SecureRandom()
        return (1..count).map {
            random.nextInt(100000000).toString().padStart(8, '0')
        }
    }

    fun generateQrCodeUrl(email: String, secret: TotpSecret, issuer: String = "Kompralo"): String {
        val encodedIssuer = java.net.URLEncoder.encode(issuer, "UTF-8")
        val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
        return "otpauth://totp/$encodedIssuer:$encodedEmail?secret=${secret.value}&issuer=$encodedIssuer"
    }

    fun verifyCode(secret: TotpSecret, code: String): Boolean {
        try {
            val currentTimeSlot = System.currentTimeMillis() / 1000 / 30

            // Verificar ventana de ±1 (compensar desincronización)
            for (i in -1..1) {
                val timeSlot = currentTimeSlot + i
                val generatedCode = generateTOTP(secret, timeSlot)
                if (code == generatedCode) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun generateTOTP(secret: TotpSecret, timeSlot: Long): String {
        val key = base32.decode(secret.value)

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
