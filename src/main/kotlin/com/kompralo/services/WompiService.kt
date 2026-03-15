package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.WompiTransactionData
import com.kompralo.dto.WompiTransactionResponse
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class WompiService(
    private val objectMapper: ObjectMapper,
) {
    @Value("\${wompi.public-key}")
    private lateinit var publicKey: String

    @Value("\${wompi.private-key}")
    private lateinit var privateKey: String

    @Value("\${wompi.integrity-secret}")
    private lateinit var integritySecret: String

    @Value("\${wompi.events-secret}")
    private lateinit var eventsSecret: String

    @Value("\${wompi.base-url}")
    private lateinit var baseUrl: String

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val lenientMapper by lazy {
        objectMapper.copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun generateReference(buyerId: Long): String {
        val ts = Instant.now().epochSecond
        val rand = UUID.randomUUID().toString().take(4).uppercase()
        return "KMP-$buyerId-$ts-$rand"
    }

    fun computeIntegritySignature(reference: String, amountInCents: Long, currency: String): String {
        val raw = "$reference$amountInCents${currency}$integritySecret"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyTransaction(transactionId: String, expectedAmountInCents: Long, reference: String): WompiTransactionData {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/transactions/$transactionId"))
            .header("Authorization", "Bearer $privateKey")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw PaymentFailedException("Error al verificar la transaccion de pago")
        }

        val txResponse = lenientMapper.readValue(response.body(), WompiTransactionResponse::class.java)
        val tx = txResponse.data

        if (tx.status != "APPROVED") {
            throw PaymentFailedException("La transaccion no fue aprobada. Estado: ${tx.status}")
        }
        if (tx.amountInCents != expectedAmountInCents) {
            throw PaymentFailedException("El monto de la transaccion no coincide con el pedido")
        }
        if (tx.reference != reference) {
            throw PaymentFailedException("La referencia de la transaccion no coincide")
        }

        return tx
    }

    fun verifyWebhookSignature(payload: Map<String, Any?>, receivedChecksum: String): Boolean {
        return try {
            @Suppress("UNCHECKED_CAST")
            val signature = payload["signature"] as? Map<String, Any?> ?: return false
            @Suppress("UNCHECKED_CAST")
            val properties = signature["properties"] as? List<String> ?: return false
            val timestamp = payload["timestamp"]?.toString() ?: return false

            @Suppress("UNCHECKED_CAST")
            val data = payload["data"] as? Map<String, Any?> ?: return false

            val concat = StringBuilder()
            for (prop in properties) {
                val parts = prop.split(".")
                var current: Any? = data
                for (part in parts) {
                    current = (current as? Map<*, *>)?.get(part)
                }
                concat.append(current?.toString() ?: "")
            }
            concat.append(timestamp)
            concat.append(eventsSecret)

            val digest = MessageDigest.getInstance("SHA-256")
            val computed = digest.digest(concat.toString().toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            MessageDigest.isEqual(computed.toByteArray(), receivedChecksum.toByteArray())
        } catch (e: Exception) {
            false
        }
    }

    fun getPublicKey(): String = publicKey

    fun toCents(amount: BigDecimal): Long {
        return amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
