package com.kompralo.controller

import com.kompralo.repository.UserRepository
import com.kompralo.services.StoreFollowerService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class StoreFollowerController(
    private val storeFollowerService: StoreFollowerService,
    private val userRepository: UserRepository
) {

    @PostMapping("/api/stores/{sellerId}/follow")
    fun followStore(
        @PathVariable sellerId: Long,
        auth: Authentication
    ): ResponseEntity<Map<String, Boolean>> {
        val user = getUser(auth)
        storeFollowerService.followStore(user, sellerId)
        return ResponseEntity.ok(mapOf("following" to true))
    }

    @DeleteMapping("/api/stores/{sellerId}/follow")
    fun unfollowStore(
        @PathVariable sellerId: Long,
        auth: Authentication
    ): ResponseEntity<Map<String, Boolean>> {
        val user = getUser(auth)
        storeFollowerService.unfollowStore(user, sellerId)
        return ResponseEntity.ok(mapOf("following" to false))
    }

    @GetMapping("/api/stores/{sellerId}/following")
    fun isFollowing(
        @PathVariable sellerId: Long,
        auth: Authentication
    ): ResponseEntity<Map<String, Boolean>> {
        val user = getUser(auth)
        return ResponseEntity.ok(mapOf("following" to storeFollowerService.isFollowing(user, sellerId)))
    }

    @GetMapping("/api/public/stores/{sellerId}/followers/count")
    fun getFollowerCount(@PathVariable sellerId: Long): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(mapOf("count" to storeFollowerService.getFollowerCount(sellerId)))
    }

    private fun getUser(auth: Authentication) =
        userRepository.findByEmail(auth.name)
            .orElseThrow { RuntimeException("Usuario no encontrado") }
}
