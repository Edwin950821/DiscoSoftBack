package com.kompralo.services

import com.kompralo.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .or { userRepository.findByUsername(username) }
            .orElseThrow { UsernameNotFoundException("Usuario no encontrado: $username") }

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.email)
            .password(user.password)
            .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            .disabled(!(user.isActive ?: true))
            .build()
    }
}
