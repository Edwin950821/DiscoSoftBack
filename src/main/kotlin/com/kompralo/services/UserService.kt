package com.kompralo.services

import com.kompralo.dto.CreateUserRequest
import com.kompralo.dto.PagedResponse
import com.kompralo.dto.UpdateUserRequest
import com.kompralo.dto.UserDTO
import com.kompralo.exception.ResourceAlreadyExistsException
import com.kompralo.exception.ResourceNotFoundException
import com.kompralo.model.Role
import com.kompralo.model.User
import com.kompralo.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * Obtiene todos los usuarios con paginación
     */
    fun getAllUsers(page: Int, size: Int, sortBy: String = "createdAt", sortDir: String = "desc"): PagedResponse<UserDTO> {
        val sort = if (sortDir.equals("asc", ignoreCase = true)) {
            Sort.by(sortBy).ascending()
        } else {
            Sort.by(sortBy).descending()
        }

        val pageable = PageRequest.of(page, size, sort)
        val usersPage = userRepository.findAll(pageable)

        return PagedResponse(
            content = usersPage.content.map { UserDTO.fromEntity(it) },
            page = usersPage.number,
            size = usersPage.size,
            totalElements = usersPage.totalElements,
            totalPages = usersPage.totalPages,
            isFirst = usersPage.isFirst,
            isLast = usersPage.isLast
        )
    }

    /**
     * Obtiene un usuario por ID
     */
    fun getUserById(id: Long): UserDTO {
        val user = userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Usuario con ID $id no encontrado") }
        return UserDTO.fromEntity(user)
    }

    /**
     * Obtiene un usuario por email
     */
    fun getUserByEmail(email: String): UserDTO {
        val user = userRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("Usuario con email $email no encontrado") }
        return UserDTO.fromEntity(user)
    }

    /**
     * Crea un nuevo usuario
     */
    @Transactional
    fun createUser(request: CreateUserRequest): UserDTO {
        // Verificar si el email ya existe
        if (userRepository.existsByEmail(request.email)) {
            throw ResourceAlreadyExistsException("El email ${request.email} ya está registrado")
        }

        request.username?.let { uname ->
            userRepository.findByUsername(uname).ifPresent {
                throw ResourceAlreadyExistsException("El username $uname ya está registrado")
            }
        }

        val user = User(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = request.role,
            isActive = request.isActive,
            username = request.username
        )

        val savedUser = userRepository.save(user)
        return UserDTO.fromEntity(savedUser)
    }

    /**
     * Actualiza un usuario existente
     */
    @Transactional
    fun updateUser(id: Long, request: UpdateUserRequest): UserDTO {
        val user = userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Usuario con ID $id no encontrado") }

        // Verificar si el nuevo email ya existe (y no es el mismo usuario)
        request.email?.let { newEmail ->
            if (newEmail != user.email && userRepository.existsByEmail(newEmail)) {
                throw ResourceAlreadyExistsException("El email $newEmail ya está registrado")
            }
        }

        // Crear copia actualizada del usuario
        val updatedUser = user.copy(
            name = request.name ?: user.name,
            email = request.email ?: user.email,
            password = request.password?.let { passwordEncoder.encode(it) } ?: user.password,
            role = request.role ?: user.role,
            isActive = request.isActive ?: user.isUserActive()
        )

        val savedUser = userRepository.save(updatedUser)
        return UserDTO.fromEntity(savedUser)
    }

    /**
     * Elimina un usuario por ID
     */
    @Transactional
    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException("Usuario con ID $id no encontrado")
        }
        userRepository.deleteById(id)
    }

    /**
     * Verifica si un usuario existe por ID
     */
    fun existsById(id: Long): Boolean {
        return userRepository.existsById(id)
    }

    /**
     * Cuenta el total de usuarios
     */
    fun countUsers(): Long {
        return userRepository.count()
    }

    /**
     * Obtiene usuarios por rol
     */
    fun getUsersByRole(role: Role): List<UserDTO> {
        return userRepository.findByRole(role).map { UserDTO.fromEntity(it) }
    }

    /**
     * Obtiene administradores y negocios
     */
    fun getAdminsAndBusinesses(): List<UserDTO> {
        return userRepository.findByRoleIn(listOf(Role.ADMIN, Role.BUSINESS))
            .map { UserDTO.fromEntity(it) }
    }
}
