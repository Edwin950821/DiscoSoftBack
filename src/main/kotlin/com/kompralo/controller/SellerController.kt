package com.kompralo.controller

import com.kompralo.dto.PublicSellerProfileResponse
import com.kompralo.dto.SalesDashboardResponse
import com.kompralo.dto.SellerProfileResponse
import com.kompralo.dto.SellerRegisterRequest
import com.kompralo.dto.UpdateSellerProfileRequest
import com.kompralo.services.SalesDashboardService
import com.kompralo.services.SellerProfileService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sellers")
class SellerController(
    private val sellerProfileService: SellerProfileService,
    private val salesDashboardService: SalesDashboardService
) {

    private fun createAuthCookie(token: String, isSecure: Boolean = false): Cookie {
        return Cookie("authToken", token).apply {
            isHttpOnly = true
            secure = isSecure
            path = "/"
            maxAge = 86400
            setAttribute("SameSite", if (isSecure) "None" else "Lax")
        }
    }

    private fun isSecureRequest(request: jakarta.servlet.http.HttpServletRequest): Boolean {
        return request.isSecure ||
            request.getHeader("X-Forwarded-Proto") == "https" ||
            request.getHeader("Origin")?.startsWith("https://") == true
    }

    @PostMapping("/register")
    fun registerSeller(
        @Valid @RequestBody request: SellerRegisterRequest,
        servletRequest: jakarta.servlet.http.HttpServletRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = sellerProfileService.registerSeller(request)
            val secure = isSecureRequest(servletRequest)
            val token = (response as? Map<*, *>)?.get("token") as? String
            if (token != null) {
                servletResponse.addCookie(createAuthCookie(token, secure))
            }

            val responseWithoutToken = (response as? Map<*, *>)?.toMutableMap()?.apply {
                remove("token")
            } ?: response

            ResponseEntity.ok(responseWithoutToken)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error en el registro de vendedor")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @GetMapping("/profile")
    fun getMySellerProfile(authentication: Authentication): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val profile = sellerProfileService.getSellerProfile(email)

            ResponseEntity.ok(profile)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Perfil de vendedor no encontrado")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @PutMapping("/profile")
    fun updateMySellerProfile(
        @Valid @RequestBody request: UpdateSellerProfileRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val profile = sellerProfileService.updateSellerProfile(email, request)

            ResponseEntity.ok(profile)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar perfil de vendedor")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @GetMapping
    fun listVerifiedSellers(): ResponseEntity<List<PublicSellerProfileResponse>> {
        return try {
            val sellers = sellerProfileService.listVerifiedSellers()

            ResponseEntity.ok(sellers)

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(emptyList())
        }
    }

    @GetMapping("/{id}")
    fun getPublicSellerProfile(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            val profile = sellerProfileService.getPublicSellerProfile(id)

            ResponseEntity.ok(profile)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to (e.message ?: "Vendedor no encontrado")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }

    @GetMapping("/sales/dashboard")
    fun getSalesDashboard(authentication: Authentication): ResponseEntity<*> {
        return try {
            val email = authentication.name
            val dashboard = salesDashboardService.getSalesDashboard(email)

            ResponseEntity.ok(dashboard)

        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al obtener dashboard")))

        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error interno del servidor"))
        }
    }
}
