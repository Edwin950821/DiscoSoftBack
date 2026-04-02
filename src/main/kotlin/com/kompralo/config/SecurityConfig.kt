package com.kompralo.config

import com.kompralo.services.CustomUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    private val userDetailsService: CustomUserDetailsService
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
        val csrfHandler = CsrfTokenRequestAttributeHandler()
        csrfHandler.setCsrfRequestAttributeName(null)

        http
            .cors { }
            .csrf { csrf ->
                csrf.csrfTokenRepository(csrfTokenRepository)
                csrf.csrfTokenRequestHandler(csrfHandler)
                csrf.ignoringRequestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/login/2fa",
                    "/api/auth/logout",
                    "/api/auth/password-reset/**",
                    "/api/auth/health",
                    "/api/sellers/register",
                    "/api/public/**",
                    "/api/webhooks/**",
                    "/api/orders/**",
                    "/api/disco/auth/**",
                    "/api/disco/management/**",
                    "/api/disco/pedidos/**",
                    "/api/disco/billar/**"
                )
            }
            .headers { headers ->
                headers.frameOptions { it.deny() }
                headers.contentTypeOptions { }
                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                    hsts.maxAgeInSeconds(31536000)
                }
                headers.cacheControl { }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/login/2fa",
                        "/api/auth/logout",
                        "/api/auth/password-reset/**",
                        "/api/auth/health",
                        "/api/sellers/register",
                        "/api/sellers",
                        "/api/sellers/{id}",
                        "/api/public/**",
                        "/api/webhooks/**",
                        "/uploads/**",
                        "/api/disco/auth/**"
                    ).permitAll()
                    .requestMatchers(
                        "/api/disco/management/**",
                        "/api/disco/pedidos/**",
                        "/api/disco/billar/**"
                    ).authenticated()
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
