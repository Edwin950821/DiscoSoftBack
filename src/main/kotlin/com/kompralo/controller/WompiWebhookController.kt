package com.kompralo.controller

import com.kompralo.services.WompiService
import com.kompralo.services.WompiWebhookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/webhooks/wompi")
class WompiWebhookController(
    private val wompiService: WompiService,
    private val wompiWebhookService: WompiWebhookService,
) {

    @PostMapping
    fun handleWebhook(
        @RequestBody payload: Map<String, Any?>,
        @RequestHeader("X-Event-Checksum", required = false) checksum: String?,
    ): ResponseEntity<*> {
        if (checksum == null || !wompiService.verifyWebhookSignature(payload, checksum)) {
            return ResponseEntity.status(401)
                .body(mapOf("message" to "Firma invalida"))
        }

        return try {
            wompiWebhookService.processEvent(payload)
            ResponseEntity.ok(mapOf("received" to true))
        } catch (e: Exception) {
            ResponseEntity.status(500)
                .body(mapOf("received" to false, "error" to (e.message ?: "Error procesando evento")))
        }
    }
}
