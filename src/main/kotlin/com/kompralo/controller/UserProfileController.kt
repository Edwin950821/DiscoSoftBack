package com.kompralo.controller

import com.kompralo.dto.UpdateUserProfileRequest
import com.kompralo.dto.UserProfileResponse
import com.kompralo.services.UserProfileService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserProfileController(
    private val userProfileService: UserProfileService
) {

    @GetMapping("/profile")
    fun getMyProfile(authentication: Authentication): ResponseEntity<UserProfileResponse> {
        return try {
            val email = authentication.name
            val profile = userProfileService.getUserProfile(email)

            ResponseEntity.ok(profile)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null)

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @PutMapping("/profile")
    fun updateMyProfile(
        @Valid @RequestBody request: UpdateUserProfileRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val profile = userProfileService.updateUserProfile(email, request)

            ResponseEntity.ok(profile)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar perfil")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @DeleteMapping("/profile")
    fun clearMyProfile(authentication: Authentication): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val profile = userProfileService.clearUserProfile(email)

            ResponseEntity.ok(
                mapOf(
                    "message" to "Perfil limpiado exitosamente",
                    "profile" to profile
                )
            )

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Perfil no encontrado")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }
}
