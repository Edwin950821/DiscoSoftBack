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
import com.kompralo.dto.UserResponse
import com.kompralo.model.Role
import com.kompralo.model.User
import com.kompralo.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Servicio de autenticación - Maneja registro, login y gestión de usuarios
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val twoFactorAuthService: TwoFactorAuthService,
    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private val googleClientId: String
) {

    /**
     * Registra un nuevo usuario en el sistema
     * @throws IllegalArgumentException si el email ya está registrado o el tipo de cuenta es inválido
     */
    fun register(request: RegisterRequest): AuthResponse {
        // Convierte accountType a Role
        val role = when (request.accountType.lowercase()) {
            "user" -> Role.USER
            "business" -> Role.BUSINESS
            else -> throw IllegalArgumentException("Tipo de cuenta inválido. Use 'user' o 'business'")
        }

        // Verifica si el usuario existe (puede estar desactivado)
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser.isPresent) {
            val user = existingUser.get()
            if (!user.isActive) {
                // Reactivar cuenta eliminada
                user.isActive = true
                user.role = role
                user.password = passwordEncoder.encode(request.password)
                val savedUser = userRepository.save(user)
                val token = jwtService.generateToken(savedUser.email, savedUser.role.name)
                return AuthResponse(token = token, user = toUserResponse(savedUser))
            }
            throw IllegalArgumentException("El email ya está registrado")
        }

        // Crea el usuario con password hasheado
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            role = role
        )

        // Guarda en base de datos
        val savedUser = userRepository.save(user)

        // Genera token JWT con rol
        val token = jwtService.generateToken(savedUser.email, savedUser.role.name)

        return AuthResponse(
            token = token,
            user = toUserResponse(savedUser)
        )
    }

    /**
     * Autentica un usuario existente
     * Si el usuario tiene 2FA habilitado, devuelve respuesta indicando que se requiere código 2FA
     * @throws IllegalArgumentException si las credenciales son inválidas
     */
    fun login(request: LoginRequest): AuthResponse {
        // Busca el usuario
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("Credenciales inválidas") }

        // Verifica password
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Credenciales inválidas")
        }

        // Verifica que esté activo
        if (!user.isActive) {
            throw IllegalArgumentException("Usuario inactivo")
        }

        // Verificar si tiene 2FA habilitado
        if (twoFactorAuthService.isTwoFactorEnabled(user)) {
            // Si tiene 2FA, no devolver token aún
            return AuthResponse(
                token = null,
                user = toUserResponse(user),
                twoFactorRequired = true,
                message = "Ingrese su código de autenticación de dos factores"
            )
        }

        // Si no tiene 2FA, generar token normalmente con rol
        val token = jwtService.generateToken(user.email, user.role.name)

        return AuthResponse(
            token = token,
            user = toUserResponse(user),
            twoFactorRequired = false
        )
    }

    /**
     * Autentica un usuario con código 2FA
     * Completa el login después de verificar el código de dos factores
     * @throws IllegalArgumentException si las credenciales o el código 2FA son inválidos
     */
    fun loginWith2FA(request: LoginWith2FARequest): AuthResponse {
        // Busca el usuario
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("Credenciales inválidas") }

        // Verifica password
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("Credenciales inválidas")
        }

        // Verifica que esté activo
        if (!user.isActive) {
            throw IllegalArgumentException("Usuario inactivo")
        }

        // Verificar código 2FA
        if (!twoFactorAuthService.verifyTwoFactorCode(user, request.twoFactorCode)) {
            throw IllegalArgumentException("Código 2FA inválido")
        }

        // Genera token JWT con rol
        val token = jwtService.generateToken(user.email, user.role.name)

        return AuthResponse(
            token = token,
            user = toUserResponse(user),
            twoFactorRequired = false
        )
    }

    /**
     * Registra un nuevo usuario con Google OAuth
     * @throws IllegalArgumentException si el email ya está registrado o el tipo de cuenta es inválido
     * @throws Exception si el token es inválido
     */
    fun googleRegister(request: GoogleRegisterRequest): AuthResponse {
        try {
            println("DEBUG: Registrando usuario con Google...")
            println("DEBUG: Client ID: $googleClientId")

            // Configurar el verificador de Google ID Token
            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(listOf(googleClientId))
                .build()

            // Verificar el token con Google
            val idToken: GoogleIdToken = verifier.verify(request.credential)
                ?: throw Exception("Token de Google inválido o expirado")

            val payload: GoogleIdToken.Payload = idToken.payload

            // Extraer información del usuario
            val email = payload.email
            val name = payload["name"] as? String ?: email
            val googleUserId = payload.subject

            println("DEBUG: Datos extraídos de Google - Email: $email, Name: $name")

            // Convertir accountType a Role según el tipo de usuario seleccionado
            val role = when (request.accountType.lowercase()) {
                "user" -> Role.USER        // Comprador
                "business" -> Role.BUSINESS // Vendedor (Comercio)
                else -> throw IllegalArgumentException("Tipo de cuenta inválido. Use 'user' (Comprador) o 'business' (Vendedor)")
            }

            // Verificar si el usuario existe (puede estar desactivado)
            val existingUser = userRepository.findByEmail(email)
            if (existingUser.isPresent) {
                val user = existingUser.get()
                if (!user.isActive) {
                    // Reactivar cuenta eliminada
                    user.isActive = true
                    user.role = role
                    val reactivated = userRepository.save(user)
                    val token = jwtService.generateToken(reactivated.email, reactivated.role.name)
                    return AuthResponse(token = token, user = toUserResponse(reactivated))
                }
                throw IllegalArgumentException("El email ya está registrado. Use el endpoint de login.")
            }

            // Crear nuevo usuario con Google
            val user = User(
                email = email,
                password = passwordEncoder.encode(googleUserId),
                name = name,
                role = role,
                createdAt = LocalDateTime.now()
            )

            val savedUser = userRepository.save(user)
            println("DEBUG: Usuario creado exitosamente: ${savedUser.email} con rol ${savedUser.role}")

            // Generar token JWT con rol
            val token = jwtService.generateToken(savedUser.email, savedUser.role.name)

            return AuthResponse(
                token = token,
                user = toUserResponse(savedUser)
            )

        } catch (e: IllegalArgumentException) {

            throw e
        } catch (e: Exception) {

            println("ERROR COMPLETO: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            throw Exception("Error al registrar con Google: ${e.message}")
        }
    }


    fun googleRegisterWithToken(request: GoogleRegisterWithTokenRequest): AuthResponse {
        println("DEBUG: Google Auth con Access Token...")
        println("DEBUG: Email: ${request.email}, Name: ${request.name}, GoogleId: ${request.getGoogleIdValue()}")


        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser.isPresent) {
            val user = existingUser.get()
            if (!user.isActive) {

                val role = when (request.accountType.lowercase()) {
                    "user" -> Role.USER
                    "business" -> Role.BUSINESS
                    else -> Role.USER
                }
                user.isActive = true
                user.role = role
                userRepository.save(user)
                println("DEBUG: Cuenta reactivada: ${user.email} con rol ${user.role}")
                val token = jwtService.generateToken(user.email, user.role.name)
                return AuthResponse(token = token, user = toUserResponse(user))
            }
            println("DEBUG: Usuario existente encontrado: ${user.email} con rol ${user.role}")
            val token = jwtService.generateToken(user.email, user.role.name)
            return AuthResponse(token = token, user = toUserResponse(user))
        }

        val role = when (request.accountType.lowercase()) {
            "user" -> Role.USER
            "business" -> Role.BUSINESS
            else -> throw IllegalArgumentException("Tipo de cuenta inválido. Use 'user' (Comprador) o 'business' (Vendedor)")
        }

        val googleIdValue = request.getGoogleIdValue().ifEmpty { request.email }
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(googleIdValue),
            name = request.name,
            role = role,
            createdAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(user)
        println("DEBUG: Usuario creado exitosamente: ${savedUser.email} con rol ${savedUser.role}")

        val token = jwtService.generateToken(savedUser.email, savedUser.role.name)

        return AuthResponse(
            token = token,
            user = toUserResponse(savedUser)
        )
    }

    fun googleLogin(credential: String): AuthResponse {
        try {
            println("DEBUG: Login con Google...")
            println("DEBUG: Client ID: $googleClientId")

            val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(listOf(googleClientId))
                .build()


            val idToken: GoogleIdToken = verifier.verify(credential)
                ?: throw Exception("Token de Google inválido o expirado")

            val payload: GoogleIdToken.Payload = idToken.payload


            val email = payload.email
            val name = payload["name"] as? String ?: email
            val googleUserId = payload.subject

            println("DEBUG: Buscando usuario: $email")

            val user = userRepository.findByEmail(email)
                .orElseThrow { IllegalArgumentException("No tienes una cuenta registrada. Por favor regístrate primero.") }

            println("DEBUG: Usuario encontrado: ${user.email} con rol ${user.role}")


            if (!user.isActive) {
                throw IllegalArgumentException("Usuario inactivo")
            }

            val token = jwtService.generateToken(user.email, user.role.name)

            return AuthResponse(
                token = token,
                user = toUserResponse(user)
            )

        } catch (e: IllegalArgumentException) {

            throw e
        } catch (e: Exception) {

            println("ERROR COMPLETO: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace()
            throw Exception("Error al autenticar con Google: ${e.message}")
        }
    }


    fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("Usuario no encontrado") }

        return toUserResponse(user)
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
        println("DEBUG: Cuenta desactivada para: ${user.email}")
    }


    private fun toUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id ?: 0,
            email = user.email,
            name = user.name,
            role = user.role,
            createdAt = user.createdAt.toString()
        )
    }
}