package com.kompralo.controller

import com.kompralo.services.CustomerService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/customers")
class CustomerController(
    private val customerService: CustomerService
) {

    @GetMapping
    fun getCustomers(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) segment: String?,
        authentication: Authentication
    ): ResponseEntity<*> {
        val email = authentication.name
        val customers = customerService.getCustomersBySeller(email, search, segment)
        return ResponseEntity.ok(mapOf("customers" to customers, "total" to customers.size))
    }

    @GetMapping("/stats")
    fun getCustomerStats(authentication: Authentication): ResponseEntity<*> {
        val email = authentication.name
        return ResponseEntity.ok(customerService.getCustomerStats(email))
    }

    @GetMapping("/export")
    fun exportCustomers(
        @RequestParam(required = false) segment: String?,
        authentication: Authentication
    ): ResponseEntity<*> {
        val email = authentication.name
        val customers = customerService.getCustomersForExport(email, segment)
        return ResponseEntity.ok(customers)
    }

    @GetMapping("/{id}")
    fun getCustomer(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        val email = authentication.name
        return ResponseEntity.ok(customerService.getCustomerById(id, email))
    }
}
