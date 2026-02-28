package com.kompralo.services

import com.kompralo.dto.*
import com.kompralo.model.Role
import com.kompralo.model.SellerProfile
import com.kompralo.model.SellerStatus
import com.kompralo.model.User
import com.kompralo.repository.SellerProfileRepository
import com.kompralo.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SellerProfileService(
    private val sellerProfileRepository: SellerProfileRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @Transactional
    fun registerSeller(request: SellerRegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("El email ya está registrado")
        }

        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            role = Role.BUSINESS
        )

        val savedUser = userRepository.save(user)

        val sellerProfile = SellerProfile(
            user = savedUser,
            businessName = request.businessName,
            businessType = request.businessType,
            description = request.description,
            phone = request.phone,
            website = request.website,
            taxId = request.taxId,
            address = request.address,
            city = request.city,
            state = request.state,
            postalCode = request.postalCode,
            country = request.country,
            status = SellerStatus.PENDING
        )

        sellerProfileRepository.save(sellerProfile)

        val token = jwtService.generateToken(savedUser.email, savedUser.role.name)

        return AuthResponse(
            token = token,
            active = savedUser.isActive,
            code = savedUser.code ?: "",
            company_uuid = savedUser.companyUuid ?: "",
            created_at = savedUser.createdAt.toString(),
            last_modified_at = savedUser.updatedAt.toString(),
            username = savedUser.username ?: savedUser.email,
            uuid = savedUser.uuid ?: "",
            role = savedUser.role.name,
            message = "Vendedor registrado exitosamente. Tu cuenta está pendiente de verificación."
        )
    }

    @Transactional(readOnly = true)
    fun getSellerProfile(email: String): SellerProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        if (user.role != Role.BUSINESS) {
            throw IllegalArgumentException("El usuario no es un vendedor")
        }

        val sellerProfile = sellerProfileRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("Perfil de vendedor no encontrado") }

        return toSellerProfileResponse(sellerProfile)
    }

    @Transactional(readOnly = true)
    fun getPublicSellerProfile(sellerId: Long): PublicSellerProfileResponse {
        val sellerProfile = sellerProfileRepository.findById(sellerId)
            .orElseThrow { IllegalArgumentException("Vendedor no encontrado") }

        return toPublicSellerProfileResponse(sellerProfile)
    }

    @Transactional
    fun updateSellerProfile(email: String, request: UpdateSellerProfileRequest): SellerProfileResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        if (user.role != Role.BUSINESS) {
            throw IllegalArgumentException("El usuario no es un vendedor")
        }

        val sellerProfile = sellerProfileRepository.findByUser(user)
            .orElseThrow { IllegalArgumentException("Perfil de vendedor no encontrado") }

        request.businessName?.let { sellerProfile.businessName = it }
        request.businessType?.let { sellerProfile.businessType = it }
        request.description?.let { sellerProfile.description = it }
        request.logoUrl?.let { sellerProfile.logoUrl = it }
        request.phone?.let { sellerProfile.phone = it }
        request.website?.let { sellerProfile.website = it }
        request.taxId?.let { sellerProfile.taxId = it }
        request.address?.let { sellerProfile.address = it }
        request.city?.let { sellerProfile.city = it }
        request.state?.let { sellerProfile.state = it }
        request.postalCode?.let { sellerProfile.postalCode = it }
        request.country?.let { sellerProfile.country = it }

        val savedProfile = sellerProfileRepository.save(sellerProfile)

        return toSellerProfileResponse(savedProfile)
    }

    @Transactional(readOnly = true)
    fun listVerifiedSellers(): List<PublicSellerProfileResponse> {
        val sellers = sellerProfileRepository.findByVerifiedTrueAndStatus(SellerStatus.ACTIVE)
        return sellers.map { toPublicSellerProfileResponse(it) }
    }

    private fun toSellerProfileResponse(profile: SellerProfile): SellerProfileResponse {
        return SellerProfileResponse(
            id = profile.id ?: 0,
            userId = profile.user.id ?: 0,
            businessName = profile.businessName,
            businessType = profile.businessType,
            description = profile.description,
            logoUrl = profile.logoUrl,
            phone = profile.phone,
            website = profile.website,
            taxId = profile.taxId,
            address = profile.address,
            city = profile.city,
            state = profile.state,
            postalCode = profile.postalCode,
            country = profile.country,
            verified = profile.verified,
            verificationDate = profile.verificationDate?.toString(),
            status = profile.status,
            totalSales = profile.totalSales,
            totalProducts = profile.totalProducts,
            averageRating = profile.averageRating,
            totalReviews = profile.totalReviews,
            createdAt = profile.createdAt.toString(),
            updatedAt = profile.updatedAt.toString()
        )
    }

    private fun toPublicSellerProfileResponse(profile: SellerProfile): PublicSellerProfileResponse {
        return PublicSellerProfileResponse(
            id = profile.id ?: 0,
            businessName = profile.businessName,
            businessType = profile.businessType,
            description = profile.description,
            logoUrl = profile.logoUrl,
            city = profile.city,
            state = profile.state,
            country = profile.country,
            verified = profile.verified,
            totalProducts = profile.totalProducts,
            averageRating = profile.averageRating,
            totalReviews = profile.totalReviews,
            memberSince = profile.createdAt.toString()
        )
    }
}
