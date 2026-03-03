package com.kompralo.controller

import com.kompralo.services.AnalyticsService
import com.kompralo.services.TrackBatchRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService,
) {

    @PostMapping("/track")
    fun track(@RequestBody request: TrackBatchRequest): ResponseEntity<Void> {
        return try {
            analyticsService.trackBatch(request.events)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            println("[Analytics] Error tracking events: ${e.message}")
            ResponseEntity.noContent().build()
        }
    }
}
