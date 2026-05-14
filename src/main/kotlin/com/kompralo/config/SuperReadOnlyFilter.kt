package com.kompralo.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(2)
class SuperReadOnlyFilter : OncePerRequestFilter() {

    private val mutatingMethods = setOf("POST", "PUT", "PATCH", "DELETE")

    private val tenantScopedPrefixes = listOf(
        "/api/disco/management/",
        "/api/disco/pedidos/",
        "/api/disco/billar/"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val auth = SecurityContextHolder.getContext().authentication
        val isSuper = auth?.authorities?.any { it.authority == "ROLE_SUPER" } == true

        if (isSuper && request.method in mutatingMethods) {
            val path = request.servletPath
            if (tenantScopedPrefixes.any { path.startsWith(it) }) {
                response.status = HttpServletResponse.SC_FORBIDDEN
                response.contentType = "application/json"
                response.characterEncoding = "UTF-8"
                response.writer.write("""{"message":"El rol SUPER es solo lectura. No puede modificar datos del negocio."}""")
                return
            }
        }

        filterChain.doFilter(request, response)
    }
}
