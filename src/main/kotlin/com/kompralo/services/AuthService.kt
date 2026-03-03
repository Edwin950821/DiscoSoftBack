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

    fun register(request: RegisterRequest): AuthResponse {
        val existingUser = userRepository.findByEmail(request.username)
        if (existingUser.isPresent) {
            throw IllegalArgumentException("Este username ya está registrado")
        }

        val companyUuid = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        val user = User(
            email = request.username,
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
            authProvider = "email",
            createdAt = now
        )

        val savedUser = userRepository.save(user)
        val token = jwtService.generateToken(savedUser.email, savedUser.role.name)

        return toAuthResponse(savedUser, token)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.resolvedEmail)
            .orElseThrow { IllegalArgumentException("Credenciales inválidas") }

        if (user.authProvider == "google") {
            throw IllegalArgumentException("Esta cuenta usa inicio de sesion con Google")
        }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Credenciales inválidas")
        }

        if (!user.isActive) {
            throw IllegalArgumentException("Usuario inactivo")
        }

        if (twoFactorAuthService.isTwoFactorEnabled(user)) {
            return toAuthResponse(user, null).copy(
                twoFactorRequired = true,
                message = "Ingrese su código de autenticación de dos factores"
            )
        }

        val token = jwtService.generateToken(user.email, user.role.name)
        return toAuthResponse(user, token)
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
        return toAuthResponse(user, token)
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
                    if (user.authProvider == null) user.authProvider = "google"
                    val reactivated = userRepository.save(user)
                    val token = jwtService.generateToken(reactivated.email, reactivated.role.name)
                    return toAuthResponse(reactivated, token)
                }
                throw IllegalArgumentException("Este correo ya está registrado")
            }

            val user = User(
                email = email,
                password = passwordEncoder.encode(googleUserId),
                name = name,
                role = role,
                authProvider = "google",
                createdAt = LocalDateTime.now()
            )

            val savedUser = userRepository.save(user)
            val token = jwtService.generateToken(savedUser.email, savedUser.role.name)
            return toAuthResponse(savedUser, token)

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
                if (user.authProvider == null) user.authProvider = "google"
                if (user.image == null && !request.picture.isNullOrBlank()) {
                    user.image = request.picture
                }
                userRepository.save(user)
                val token = jwtService.generateToken(user.email, user.role.name)
                return toAuthResponse(user, token)
            }
            if (request.isLogin) {
                var needsSave = false
                if (user.authProvider == null) { user.authProvider = "google"; needsSave = true }
                if (user.image == null && !request.picture.isNullOrBlank()) { user.image = request.picture; needsSave = true }
                if (needsSave) userRepository.save(user)
                val token = jwtService.generateToken(user.email, user.role.name)
                return toAuthResponse(user, token)
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
            image = request.picture,
            authProvider = "google",
            createdAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(user)
        val token = jwtService.generateToken(savedUser.email, savedUser.role.name)
        return toAuthResponse(savedUser, token)
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

            if (user.authProvider == null) {
                user.authProvider = "google"
                userRepository.save(user)
            }

            val token = jwtService.generateToken(user.email, user.role.name)
            return toAuthResponse(user, token)

        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            log.error("Error en Google login: ${e.message}", e)
            throw Exception("Error al autenticar con Google: ${e.message}")
        }
    }

    @org.springframework.transaction.annotation.Transactional
    fun updateProfilePicture(email: String, imageUrl: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }
        user.image = imageUrl
        userRepository.save(user)
        return toAuthResponse(user, null)
    }

    fun getUserByEmail(email: String): AuthResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }
        return toAuthResponse(user, null)
    }

    fun extractEmailFromToken(token: String): String {
        return jwtService.extractEmailFromToken(token)
    }

    @org.springframework.transaction.annotation.Transactional
    fun changePassword(email: String, currentPassword: String?, newPassword: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        val requiresCurrentPassword = user.authProvider == "email" || user.authProvider == "both"

        if (requiresCurrentPassword) {
            if (currentPassword.isNullOrBlank()) {
                throw IllegalArgumentException("La contrasena actual es requerida")
            }
            if (!passwordEncoder.matches(currentPassword, user.password)) {
                throw IllegalArgumentException("La contrasena actual es incorrecta")
            }
            if (currentPassword == newPassword) {
                throw IllegalArgumentException("La nueva contrasena debe ser diferente a la actual")
            }
        }

        if (newPassword.length < 8) {
            throw IllegalArgumentException("La nueva contrasena debe tener al menos 8 caracteres")
        }

        user.password = passwordEncoder.encode(newPassword)
        if (user.authProvider != "email") {
            user.authProvider = "both"
        }
        userRepository.save(user)
    }

    @org.springframework.transaction.annotation.Transactional
    fun deleteAccount(email: String) {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }
        user.isActive = false
        userRepository.save(user)
    }

    private fun toAuthResponse(user: User, token: String?): AuthResponse {
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
            default_currency = user.defaultCurrency ?: "COP",
            auth_provider = user.authProvider
        )
    }
}
