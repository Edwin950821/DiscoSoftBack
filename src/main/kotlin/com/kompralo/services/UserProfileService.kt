package com.kompralo.services

import com.kompralo.dto.UpdateUserProfileRequest
import com.kompralo.dto.UserProfileResponse
import com.kompralo.model.User
import com.kompralo.model.UserProfile
import com.kompralo.repository.UserProfileRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Servicio para gestión de perfiles de usuario (compradores)
 */
@Service
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val userRepository: UserRepository
) {

    /**
     * Obtiene el perfil de un usuario por email
     * Si no existe, crea uno nuevo con valores por defecto
     */
    @Transactional
    fun getUserProfile(email: String): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profile = userProfileRepository.findByUser(user)
            .orElseGet {
                // Crear perfil automáticamente si no existe
                val newProfile = UserProfile(user = user)
                userProfileRepository.save(newProfile)
            }

        return toUserProfileResponse(profile)
    }

    /**
     * Actualiza el perfil de un usuario
     */
    @Transactional
    fun updateUserProfile(email: String, request: UpdateUserProfileRequest): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profile = userProfileRepository.findByUser(user)
            .orElseGet {
                // Crear perfil si no existe
                UserProfile(user = user)
            }

        // Actualizar campos (solo si se proporcionan)
        request.phone?.let { profile.phone = it }
        request.avatarUrl?.let { profile.avatarUrl = it }
        request.address?.let { profile.address = it }
        request.city?.let { profile.city = it }
        request.state?.let { profile.state = it }
        request.postalCode?.let { profile.postalCode = it }
        request.country?.let { profile.country = it }

        val savedProfile = userProfileRepository.save(profile)

        return toUserProfileResponse(savedProfile)
    }

    /**
     * Elimina el perfil de un usuario (soft delete conceptual, limpia datos)
     */
    @Transactional
    fun clearUserProfile(email: String): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profile = userProfileRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("Perfil no encontrado") }

        // Limpiar todos los datos del perfil
        profile.phone = null
        profile.avatarUrl = null
        profile.address = null
        profile.city = null
        profile.state = null
        profile.postalCode = null
        profile.country = "Colombia"

        val savedProfile = userProfileRepository.save(profile)

        return toUserProfileResponse(savedProfile)
    }

    /**
     * Convierte UserProfile a UserProfileResponse
     */
    private fun toUserProfileResponse(profile: UserProfile): UserProfileResponse {
        return UserProfileResponse(
            id = profile.id ?: 0,
            userId = profile.user.id ?: 0,
            phone = profile.phone,
            avatarUrl = profile.avatarUrl,
            address = profile.address,
            city = profile.city,
            state = profile.state,
            postalCode = profile.postalCode,
            country = profile.country,
            createdAt = profile.createdAt.toString(),
            updatedAt = profile.updatedAt.toString()
        )
    }
}
