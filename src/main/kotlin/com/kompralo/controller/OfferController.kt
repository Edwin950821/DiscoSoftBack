package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.model.Role
import com.kompralo.repository.UserRepository
import com.kompralo.services.OfferService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class OfferController(
    private val offerService: OfferService,
    private val userRepository: UserRepository
) {

    // ========== Store endpoints (BUSINESS role) ==========

    @PostMapping("/api/offers")
    fun createOffer(
        @RequestBody request: CreateOfferRequest,
        auth: Authentication
    ): ResponseEntity<OfferResponse> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.createOffer(user, request))
    }

    @PutMapping("/api/offers/{id}")
    fun updateOffer(
        @PathVariable id: Long,
        @RequestBody request: UpdateOfferRequest,
        auth: Authentication
    ): ResponseEntity<OfferResponse> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.updateOffer(id, user, request))
    }

    @DeleteMapping("/api/offers/{id}")
    fun cancelOffer(
        @PathVariable id: Long,
        auth: Authentication
    ): ResponseEntity<Void> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS) {
            return ResponseEntity.status(403).build()
        }
        offerService.cancelOffer(id, user)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/api/offers/my")
    fun getMyOffers(auth: Authentication): ResponseEntity<List<OfferSummaryResponse>> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.getOffersForSeller(user))
    }

    @GetMapping("/api/offers/my/stats")
    fun getMyOfferStats(auth: Authentication): ResponseEntity<OfferStatsResponse> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.getOfferStats(user))
    }

    // ========== Admin endpoints (ADMIN role) ==========

    @PostMapping("/api/offers/global")
    fun createGlobalOffer(
        @RequestBody request: CreateOfferRequest,
        auth: Authentication
    ): ResponseEntity<OfferResponse> {
        val user = getUser(auth)
        if (user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.createOffer(null, request))
    }

    @GetMapping("/api/offers/all")
    fun getAllOffers(auth: Authentication): ResponseEntity<List<OfferSummaryResponse>> {
        val user = getUser(auth)
        if (user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.getAllOffers())
    }

    @PostMapping("/api/special-days")
    fun createSpecialDay(
        @RequestBody request: SpecialDayRequest,
        auth: Authentication
    ): ResponseEntity<SpecialDayResponse> {
        val user = getUser(auth)
        if (user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.createSpecialDay(request))
    }

    @GetMapping("/api/special-days")
    fun getSpecialDays(auth: Authentication): ResponseEntity<List<SpecialDayResponse>> {
        return ResponseEntity.ok(offerService.getSpecialDays())
    }

    @PutMapping("/api/special-days/{id}")
    fun updateSpecialDay(
        @PathVariable id: Long,
        @RequestBody request: SpecialDayRequest,
        auth: Authentication
    ): ResponseEntity<SpecialDayResponse> {
        val user = getUser(auth)
        if (user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.updateSpecialDay(id, request))
    }

    @DeleteMapping("/api/special-days/{id}")
    fun deleteSpecialDay(
        @PathVariable id: Long,
        auth: Authentication
    ): ResponseEntity<Void> {
        val user = getUser(auth)
        if (user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        offerService.deleteSpecialDay(id)
        return ResponseEntity.ok().build()
    }

    // ========== Public endpoints (for buyers) ==========

    @GetMapping("/api/public/offers/active")
    fun getActiveOffers(): ResponseEntity<List<OfferResponse>> {
        return ResponseEntity.ok(offerService.getActiveOffers())
    }

    @GetMapping("/api/public/offers/product/{productId}")
    fun getOffersForProduct(@PathVariable productId: Long): ResponseEntity<List<ApplicableOfferResponse>> {
        return ResponseEntity.ok(offerService.getApplicableOffersForProduct(productId))
    }

    @GetMapping("/api/public/special-days")
    fun getUpcomingSpecialDays(): ResponseEntity<List<SpecialDayResponse>> {
        return ResponseEntity.ok(offerService.getUpcomingSpecialDays())
    }

    private fun getUser(auth: Authentication) =
        userRepository.findByEmail(auth.name)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
}
