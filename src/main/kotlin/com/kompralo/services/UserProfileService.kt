package com.kompralo.services

import com.kompralo.dto.UpdateUserProfileRequest
import com.kompralo.dto.UserProfileResponse
import com.kompralo.model.User
import com.kompralo.model.UserProfile
import com.kompralo.repository.UserProfileRepository
import com.kompralo.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun getUserProfile(email: String): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profile = userProfileRepository.findByUser(user)
            .orElseGet {
                val newProfile = UserProfile(user = user)
                userProfileRepository.save(newProfile)
            }

        return toUserProfileResponse(profile)
    }

    @Transactional
    fun updateUserProfile(email: String, request: UpdateUserProfileRequest): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profile = userProfileRepository.findByUser(user)
            .orElseGet {
                UserProfile(user = user)
            }

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

    @Transactional
    fun clearUserProfile(email: String): UserProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val profile = userProfileRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("Perfil no encontrado") }

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
