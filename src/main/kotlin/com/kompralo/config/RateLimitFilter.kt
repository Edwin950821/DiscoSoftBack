package com.kompralo.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val authBuckets = ConcurrentHashMap<String, Bucket>()
    private val generalBuckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val clientIp = request.remoteAddr ?: "unknown"
        val path = request.requestURI

        val isAuthEndpoint = path.startsWith("/api/auth/login")
            || path.startsWith("/api/auth/register")
            || path == "/api/auth/google/register-with-token"
            || path.startsWith("/api/auth/password-reset")
            || path.startsWith("/api/sellers/register")

        val bucket = if (isAuthEndpoint) {
            authBuckets.computeIfAbsent("$clientIp:$path") { createAuthBucket() }
        } else {
            generalBuckets.computeIfAbsent(clientIp) { createGeneralBucket() }
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.writer.write("""{"message":"Demasiadas solicitudes. Intenta de nuevo en unos minutos."}""")
        }
    }

    private fun createAuthBucket(): Bucket {
        val limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)))
        return Bucket.builder().addLimit(limit).build()
    }

    private fun createGeneralBucket(): Bucket {
        val limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)))
        return Bucket.builder().addLimit(limit).build()
    }
}
