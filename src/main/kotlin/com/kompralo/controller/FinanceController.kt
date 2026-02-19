package com.kompralo.controller

import com.kompralo.services.FinanceService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class FinanceController(
    private val financeService: FinanceService,
) {

    private fun handle(auth: Authentication, block: (String) -> Any): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(block(auth.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error en finanzas")))
        }
    }

    @GetMapping("/summary")
    fun getSummary(auth: Authentication) = handle(auth) { financeService.getSummary(it) }

    @GetMapping("/income-expense")
    fun getIncomeExpense(auth: Authentication) = handle(auth) { financeService.getIncomeExpense(it) }

    @GetMapping("/projection")
    fun getProjection(auth: Authentication) = handle(auth) { financeService.getProjection(it) }

    @GetMapping("/monthly")
    fun getMonthly(auth: Authentication) = handle(auth) { financeService.getMonthlyHistory(it) }

    @GetMapping("/costs")
    fun getCosts(auth: Authentication) = handle(auth) { financeService.getCostBreakdown(it) }

    @GetMapping("/category-profit")
    fun getCategoryProfit(auth: Authentication) = handle(auth) { financeService.getCategoryProfitability(it) }

    @GetMapping("/inventory")
    fun getInventory(auth: Authentication) = handle(auth) { financeService.getInventoryFinance(it) }

    @GetMapping("/slow-moving")
    fun getSlowMoving(auth: Authentication) = handle(auth) { financeService.getSlowMovingProducts(it) }

    @GetMapping("/stock-projection")
    fun getStockProjection(auth: Authentication) = handle(auth) { financeService.getStockProjection(it) }

    @GetMapping("/customer-roi")
    fun getCustomerROI(auth: Authentication) = handle(auth) { financeService.getCustomerROI(it) }

    @GetMapping("/segments")
    fun getSegments(auth: Authentication) = handle(auth) { financeService.getFinancialSegments(it) }

    @GetMapping("/tax-summary")
    fun getTaxSummary(auth: Authentication) = handle(auth) { financeService.getTaxSummary(it) }

    @GetMapping("/retentions")
    fun getRetentions(auth: Authentication) = handle(auth) { financeService.getTaxRetentions(it) }

    @GetMapping("/tax-trend")
    fun getTaxTrend(auth: Authentication) = handle(auth) { financeService.getTaxTrend(it) }

    @GetMapping("/channels")
    fun getChannels(auth: Authentication) = handle(auth) { financeService.getChannelData(it) }

    @GetMapping("/regions")
    fun getRegions(auth: Authentication) = handle(auth) { financeService.getRegionalPerformance(it) }

    @GetMapping("/channel-summary")
    fun getChannelSummary(auth: Authentication) = handle(auth) { financeService.getChannelSummary(it) }
}
