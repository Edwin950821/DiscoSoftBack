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

/**
 * Controlador REST para vendedores
 *
 * Endpoints públicos:
 * - POST /api/sellers/register - Registrar nuevo vendedor
 * - GET  /api/sellers - Listar vendedores verificados
 * - GET  /api/sellers/{id} - Ver perfil público de vendedor
 *
 * Endpoints privados (requieren autenticación):
 * - GET  /api/sellers/profile - Mi perfil de vendedor
 * - PUT  /api/sellers/profile - Actualizar mi perfil de vendedor
 */
@RestController
@RequestMapping("/api/sellers")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class SellerController(
    private val sellerProfileService: SellerProfileService,
    private val salesDashboardService: SalesDashboardService
) {

    /**
     * Crea una cookie HTTP-only con el token JWT
     */
    private fun createAuthCookie(token: String): Cookie {
        return Cookie("authToken", token).apply {
            isHttpOnly = true
            secure = false
            path = "/"
            maxAge = -1
        }
    }

    /**
     * Registra un nuevo vendedor
     *
     * Ruta: POST /api/sellers/register
     *
     * @param request Datos de registro del vendedor
     * @param servletResponse Response HTTP para agregar la cookie
     * @return AuthResponse con token en cookie
     */
    @PostMapping("/register")
    fun registerSeller(
        @Valid @RequestBody request: SellerRegisterRequest,
        servletResponse: HttpServletResponse
    ): ResponseEntity<*> {
        return try {
            val response = sellerProfileService.registerSeller(request)

            // Extraer token y guardarlo en cookie
            val token = (response as? Map<*, *>)?.get("token") as? String
            if (token != null) {
                servletResponse.addCookie(createAuthCookie(token))
            }

            // Remover token del response body
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

    /**
     * Obtiene el perfil del vendedor autenticado
     *
     * Ruta: GET /api/sellers/profile
     *
     * @param authentication Usuario autenticado
     * @return Perfil completo del vendedor (datos privados)
     */
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

    /**
     * Actualiza el perfil del vendedor autenticado
     *
     * Ruta: PUT /api/sellers/profile
     *
     * @param request Datos a actualizar
     * @param authentication Usuario autenticado
     * @return Perfil actualizado
     */
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

    /**
     * Lista todos los vendedores verificados y activos (público)
     *
     * Ruta: GET /api/sellers
     *
     * @return Lista de perfiles públicos de vendedores
     */
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

    /**
     * Obtiene el perfil público de un vendedor por ID
     *
     * Ruta: GET /api/sellers/{id}
     *
     * @param id ID del vendedor
     * @return Perfil público del vendedor (sin datos sensibles)
     */
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

    /**
     * Obtiene el dashboard de ventas del vendedor autenticado
     *
     * Ruta: GET /api/sellers/sales/dashboard
     *
     * Calcula:
     * - Ventas de hoy (count y sum)
     * - Ventas de la semana (count y sum)
     * - Ventas del mes (count y sum)
     * - Ticket promedio del mes
     * - Porcentaje de cambio vs período anterior
     * - Conteo de órdenes por estado (pendientes, procesando, en tránsito, entregadas)
     * - Totales históricos
     *
     * @param authentication Usuario autenticado
     * @return Dashboard con métricas de ventas
     */
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
