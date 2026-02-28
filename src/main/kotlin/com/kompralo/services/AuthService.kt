package com.kompralo.services

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.kompralo.dto.AuthResponse
import com.kompralo.dto.GoogleRegisterRequest
import com.kompralo.dto.GoogleRegisterWithTokenRequest
import com.kompralo.dto.LoginRequest
import com.kompralo.dto.LoginWith2FARequest
import com.kompralo.dto.RegisterRequest
import com.kompralo.dto.SubscriptionResponse
import com.kompralo.model.Role
import com.kompralo.model.User
import com.kompralo.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val twoFactorAuthService: TwoFactorAuthService,
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val googleClientId: String
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    private fun parseRole(accountType: String): Role = when (accountType.lowercase()) {
        "user" -> Role.USER
        "business" -> Role.BUSINESS
        "owner" -> Role.OWNER
        else -> throw IllegalArgumentException("Tipo de cuenta inválido. Use 'user', 'business' o 'owner'")
    }

    /**
     * VigXa registration: uses username as the primary identifier.
     * We store username in both `email` (for backward compat with Kompralo lookups)
     * and `username` field.
     */
    fun register(request: RegisterRequest): AuthResponse {
        // Check if username already exists (stored in email column)
        val existingUser = userRepository.findByEmail(request.username)
        if (existingUser.isPresent) {
            throw IllegalArgumentException("Este username ya está registrado")
        }

        val companyUuid = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        val user = User(
            email = request.username, // username is the primary identifier, stored in email for compat
            password = passwordEncoder.encode(request.password),
            name = "${request.owner_name ?: ""} ${request.owner_lastname ?: ""}".trim().ifEmpty { request.company_name },
            role = Role.OWNER,
            uuid = UUID.randomUUID().toString(),
            code = "USR-${System.currentTimeMillis()}",
            username = request.username,
            companyUuid = companyUuid,
            companyName = request.company_name,
            companyEmail = request.company_email,
            companyPhone = request.company_phone,
            companyNit = request.company_nit,
            ownerName = request.owner_name,
            ownerLastname = request.owner_lastname,
            ownerEmail = request.owner_email,
            plan = request.plan ?: "FREE_TRIAL",
            defaultCurrency = "COP",
            createdAt = now
        )

        val savedUser = userRepository.save(user)
        val token = jwtService.generateToken(savedUser.email, savedUser.role.name)

        return toVigxaAuthResponse(savedUser, token)
    }

    /**
     * VigXa login: looks up by username (stored in email column).
     */
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.username)
            .orElseThrow { IllegalArgumentException("Credenciales inválidas") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Credenciales inválidas")
        }

        if (!user.isActive) {
            throw IllegalArgumentException("Usuario inactivo")
        }

        if (twoFactorAuthService.isTwoFactorEnabled(user)) {
            return toVigxaAuthResponse(user, null).copy(
                twoFactorRequired = true,
                message = "Ingrese su código de autenticación de dos factores"
            )
        }

        val token = jwtService.generateToken(user.email, user.role.name)
        return toVigxaAuthResponse(user, token)
    }

    fun loginWith2FA(request: LoginWith2FARequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("Credenciales inválidas") }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Credenciales inválidas")
        }

        if (!user.isActive) {
            throw IllegalArgumentException("Usuario inactivo")
        }

        if (!twoFactorAuthService.verifyTwoFactorCode(user, request.twoFactorCode)) {
            throw IllegalArgumentException("Código 2FA inválido")
        }

        val token = jwtService.generateToken(user.email, user.role.name)
        return toVigxaAuthResponse(user, token)
    }

    fun googleRegister(request: GoogleRegisterRequest): AuthResponse {
        try {
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(listOf(googleClientId))
                .build()

            val idToken: GoogleIdToken = verifier.verify(request.credential)
                ?: throw Exception("Token de Google inválido o expirado")

            val payload: GoogleIdToken.Payload = idToken.payload
            val email = payload.email
            val name = payload["name"] as? String ?: email
            val googleUserId = payload.subject
            val role = parseRole(request.accountType)

            val existingUser = userRepository.findByEmail(email)
            if (existingUser.isPresent) {
                val user = existingUser.get()
                if (!user.isActive) {
                    user.isActive = true
                    user.role = role
                    val reactivated = userRepository.save(user)
                    val token = jwtService.generateToken(reactivated.email, reactivated.role.name)
                    return toVigxaAuthResponse(reactivated, token)
                }
                throw IllegalArgumentException("Este correo ya está registrado")
            }

            val user = User(
                email = email,
                password = passwordEncoder.encode(googleUserId),
                name = name,
                role = role,
                createdAt = LocalDateTime.now()
            )

            val savedUser = userRepository.save(user)
            val token = jwtService.generateToken(savedUser.email, savedUser.role.name)
            return toVigxaAuthResponse(savedUser, token)

        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            log.error("Error en Google register: ${e.message}", e)
            throw Exception("Error al registrar con Google: ${e.message}")
        }
    }

    fun googleRegisterWithToken(request: GoogleRegisterWithTokenRequest): AuthResponse {
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser.isPresent) {
            val user = existingUser.get()
            if (!user.isActive) {
                val role = try { parseRole(request.accountType) } catch (_: Exception) { Role.USER }
                user.isActive = true
                user.role = role
                userRepository.save(user)
                val token = jwtService.generateToken(user.email, user.role.name)
                return toVigxaAuthResponse(user, token)
            }
            if (request.isLogin) {
                val token = jwtService.generateToken(user.email, user.role.name)
                return toVigxaAuthResponse(user, token)
            }
            throw IllegalArgumentException("Este correo ya está registrado")
        }

        if (request.isLogin) {
            throw IllegalArgumentException("No tienes una cuenta registrada. Por favor regístrate primero.")
        }

        val role = parseRole(request.accountType)

        val googleIdValue = request.getGoogleIdValue().ifEmpty { request.email }
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(googleIdValue),
            name = request.name,
            role = role,
            createdAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(user)
        val token = jwtService.generateToken(savedUser.email, savedUser.role.name)
        return toVigxaAuthResponse(savedUser, token)
    }

    fun googleLogin(credential: String): AuthResponse {
        try {
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(listOf(googleClientId))
                .build()

            val idToken: GoogleIdToken = verifier.verify(credential)
                ?: throw Exception("Token de Google inválido o expirado")

            val payload: GoogleIdToken.Payload = idToken.payload
            val email = payload.email

            val user = userRepository.findByEmail(email)
                .orElseThrow { IllegalArgumentException("No tienes una cuenta registrada. Por favor regístrate primero.") }

            if (!user.isActive) {
                throw IllegalArgumentException("Usuario inactivo")
            }

            val token = jwtService.generateToken(user.email, user.role.name)
            return toVigxaAuthResponse(user, token)

        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            log.error("Error en Google login: ${e.message}", e)
            throw Exception("Error al autenticar con Google: ${e.message}")
        }
    }

    fun getUserByEmail(email: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }
        return toVigxaAuthResponse(user, null)
    }

    fun extractEmailFromToken(token: String): String {
        return jwtService.extractEmailFromToken(token)
    }

    @org.springframework.transaction.annotation.Transactional
    fun deleteAccount(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }
        user.isActive = false
        userRepository.save(user)
    }

    /**
     * Builds a flat AuthResponse matching the VigXa frontend contract.
     */
    private fun toVigxaAuthResponse(user: User, token: String?): AuthResponse {
        val now = LocalDateTime.now()
        val trialEnd = now.plusDays(15)

        return AuthResponse(
            token = token,
            active = user.isActive,
            code = user.code ?: "",
            company_uuid = user.companyUuid ?: "",
            created_at = user.createdAt.toString(),
            last_modified_at = user.updatedAt.toString(),
            username = user.username ?: user.email,
            uuid = user.uuid ?: "",
            image = user.image,
            employee_uuid = user.employeeUuid,
            role = user.role.name,
            id = user.id,
            email = user.email,
            name = user.name,
            subscription = SubscriptionResponse(
                uuid = UUID.randomUUID().toString(),
                company_uuid = user.companyUuid ?: "",
                plan = user.plan ?: "FREE_TRIAL",
                status = "TRIAL",
                trial_start_date = now.toString(),
                trial_end_date = trialEnd.toString(),
                days_remaining = 15,
                is_active = true
            ),
            permissions = listOf("ALL"),
            default_currency = user.defaultCurrency ?: "COP"
        )
    }
}
