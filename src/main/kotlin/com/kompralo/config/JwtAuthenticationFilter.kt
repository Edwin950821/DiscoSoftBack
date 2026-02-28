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

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val publicPaths = listOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/google/register",
            "/api/auth/logout",
            "/api/auth/password-reset",
            "/api/auth/health"
        )

        val requestPath = request.servletPath
        if (publicPaths.any { requestPath.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = request.cookies?.firstOrNull { it.name == "authToken" }?.value
            ?: run {
                val authHeader = request.getHeader("Authorization")
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader.substring(7)
                } else {
                    null
                }
            }

        if (jwt == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val username = jwtService.extractUsername(jwt)

            if (SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userDetailsService.loadUserByUsername(username)

                if (jwtService.validateToken(jwt)) {
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        } catch (e: Exception) {
            logger.error("Error al procesar token JWT: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }
}
