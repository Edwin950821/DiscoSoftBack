package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.model.OfferType
import com.kompralo.model.Role
import com.kompralo.repository.UserRepository
import com.kompralo.services.OfferEmailService
import com.kompralo.services.OfferService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class OfferController(
    private val offerService: OfferService,
    private val offerEmailService: OfferEmailService,
    private val userRepository: UserRepository
) {

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

    @GetMapping("/api/offers/email-history")
    fun getEmailHistory(auth: Authentication): ResponseEntity<List<EmailCampaignSummary>> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerEmailService.getCampaignHistory(user))
    }

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
        if (user.role != Role.BUSINESS && user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.createSpecialDay(user, request))
    }

    @GetMapping("/api/special-days")
    fun getSpecialDays(auth: Authentication): ResponseEntity<List<SpecialDayResponse>> {
        val user = getUser(auth)
        return if (user.role == Role.ADMIN) {
            ResponseEntity.ok(offerService.getSpecialDays())
        } else {
            ResponseEntity.ok(offerService.getSpecialDaysForSeller(user))
        }
    }

    @PutMapping("/api/special-days/{id}")
    fun updateSpecialDay(
        @PathVariable id: Long,
        @RequestBody request: SpecialDayRequest,
        auth: Authentication
    ): ResponseEntity<SpecialDayResponse> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS && user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        return ResponseEntity.ok(offerService.updateSpecialDay(id, user, request))
    }

    @DeleteMapping("/api/special-days/{id}")
    fun deleteSpecialDay(
        @PathVariable id: Long,
        auth: Authentication
    ): ResponseEntity<Void> {
        val user = getUser(auth)
        if (user.role != Role.BUSINESS && user.role != Role.ADMIN) {
            return ResponseEntity.status(403).build()
        }
        offerService.deleteSpecialDay(id, user)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/api/public/offers/active")
    fun getActiveOffers(): ResponseEntity<List<OfferResponse>> {
        return ResponseEntity.ok(offerService.getActiveOffers())
    }

    @GetMapping("/api/public/offers/browse")
    fun browseOffers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
        @RequestParam(required = false) type: OfferType?,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<OfferPageResponse> {
        return ResponseEntity.ok(offerService.browseActiveOffers(page, size.coerceAtMost(50), type, category))
    }

    @GetMapping("/api/public/offers/upcoming")
    fun getUpcomingOffers(): ResponseEntity<List<OfferResponse>> {
        return ResponseEntity.ok(offerService.getUpcomingOffers())
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
