package com.kompralo.services

import com.kompralo.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Implementación de UserDetailsService para Spring Security
 * Carga usuarios desde la base de datos para autenticación
 */
@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    /**
     * Carga un usuario por email (usado como username)
     * Este método es llamado automáticamente por Spring Security
     */
    override fun loadUserByUsername(username: String): UserDetails {
        // Busca el usuario en la base de datos
        val user = userRepository.findByEmail(username)
            .orElseThrow {
                UsernameNotFoundException("Usuario no encontrado: $username")
            }

        // Convierte el rol a authority de Spring Security
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))

        // Retorna UserDetails para Spring Security
        return org.springframework.security.core.userdetails.User(
            user.email,
            user.password,
            user.isActive,
            true, // accountNonExpired
            true, // credentialsNonExpired
            true, // accountNonLocked
            authorities
        )
    }
}