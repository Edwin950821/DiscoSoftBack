package com.kompralo.config

import com.kompralo.services.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filtro JWT para validar tokens en cada request
 * Se ejecuta antes que los filtros de Spring Security
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    /**
     * Procesa cada request y valida el token JWT si existe
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Rutas públicas que no requieren autenticación
        val publicPaths = listOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/google/register",
            "/api/auth/logout",
            "/api/auth/password-reset",
            "/api/auth/health"
        )

        // Si es una ruta pública, continúa sin validar token
        val requestPath = request.servletPath
        if (publicPaths.any { requestPath.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        // Intenta obtener el token de la cookie HTTP-only primero
        val jwt = request.cookies?.firstOrNull { it.name == "authToken" }?.value
            ?: run {
                // Si no hay cookie, intenta obtenerlo del header Authorization (fallback)
                val authHeader = request.getHeader("Authorization")
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader.substring(7)
                } else {
                    null
                }
            }

        // Si no hay token (ni en cookie ni en header), continúa sin autenticar
        if (jwt == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            // Extrae el username (email) del token
            val username = jwtService.extractUsername(jwt)

            // Si no hay autenticación en el contexto de seguridad
            if (SecurityContextHolder.getContext().authentication == null) {
                // Carga los detalles del usuario
                val userDetails = userDetailsService.loadUserByUsername(username)

                // Valida el token
                if (jwtService.validateToken(jwt)) {
                    // Crea el objeto de autenticación
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

                    // Establece la autenticación en el contexto de seguridad
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        } catch (e: Exception) {
            logger.error("Error al procesar token JWT: ${e.message}")
        }

        // Continúa con la cadena de filtros
        filterChain.doFilter(request, response)
    }
}