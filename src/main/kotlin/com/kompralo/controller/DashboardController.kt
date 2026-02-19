package com.kompralo.controller

import com.kompralo.services.DashboardService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class DashboardController(
    private val dashboardService: DashboardService,
) {

    @GetMapping("/metrics")
    fun getMetrics(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(dashboardService.getMetrics(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener metricas")))
        }
    }

    @GetMapping("/daily-income")
    fun getDailyIncome(
        @RequestParam(defaultValue = "7") days: Int,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val safeDays = days.coerceIn(1, 30)
            ResponseEntity.ok(dashboardService.getDailyIncome(authentication.name, safeDays))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener ingresos diarios")))
        }
    }

    @GetMapping("/top-categories")
    fun getTopCategories(
        @RequestParam(defaultValue = "5") limit: Int,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val safeLimit = limit.coerceIn(1, 20)
            ResponseEntity.ok(dashboardService.getTopCategories(authentication.name, safeLimit))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener categorias")))
        }
    }

    @GetMapping("/stock-summary")
    fun getStockSummary(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(dashboardService.getStockSummary(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener resumen de inventario")))
        }
    }

    @GetMapping("/region-sales")
    fun getRegionSales(
        @RequestParam(defaultValue = "5") limit: Int,
        authentication: Authentication,
    ): ResponseEntity<*> {
        return try {
            val safeLimit = limit.coerceIn(1, 20)
            ResponseEntity.ok(dashboardService.getRegionSales(authentication.name, safeLimit))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener ventas por region")))
        }
    }

    @GetMapping("/customers")
    fun getCustomerSummary(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(dashboardService.getCustomerSummary(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener resumen de clientes")))
        }
    }
}
