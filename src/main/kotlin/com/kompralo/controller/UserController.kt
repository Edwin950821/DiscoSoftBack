package com.kompralo.controller

import com.kompralo.dto.CreateUserRequest
import com.kompralo.dto.PagedResponse
import com.kompralo.dto.UpdateUserRequest
import com.kompralo.dto.UserDTO
import com.kompralo.exception.ResourceAlreadyExistsException
import com.kompralo.exception.ResourceNotFoundException
import com.kompralo.model.Role
import com.kompralo.services.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para gestión de usuarios (CRUD)
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class UserController(
    private val userService: UserService
) {

    /**
     * GET /api/users
     * Lista todos los usuarios con paginación
     *
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 10)
     * @param sortBy Campo para ordenar (default: createdAt)
     * @param sortDir Dirección del orden: asc o desc (default: desc)
     */
    @GetMapping
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String
    ): ResponseEntity<PagedResponse<UserDTO>> {
        val users = userService.getAllUsers(page, size, sortBy, sortDir)
        return ResponseEntity.ok(users)
    }

    /**
     * GET /api/users/{id}
     * Obtiene un usuario por ID
     */
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            val user = userService.getUserById(id)
            ResponseEntity.ok(user)
        } catch (e: ResourceNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        }
    }

    /**
     * GET /api/users/email/{email}
     * Obtiene un usuario por email
     */
    @GetMapping("/email/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<*> {
        return try {
            val user = userService.getUserByEmail(email)
            ResponseEntity.ok(user)
        } catch (e: ResourceNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        }
    }

    /**
     * POST /api/users
     * Crea un nuevo usuario
     */
    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<*> {
        return try {
            val user = userService.createUser(request)
            ResponseEntity.status(HttpStatus.CREATED).body(user)
        } catch (e: ResourceAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al crear usuario")))
        }
    }

    /**
     * PUT /api/users/{id}
     * Actualiza un usuario existente
     */
    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<*> {
        return try {
            val user = userService.updateUser(id, request)
            ResponseEntity.ok(user)
        } catch (e: ResourceNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: ResourceAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar usuario")))
        }
    }

    /**
     * DELETE /api/users/{id}
     * Elimina un usuario
     */
    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<*> {
        return try {
            userService.deleteUser(id)
            ResponseEntity.ok(mapOf("message" to "Usuario eliminado exitosamente"))
        } catch (e: ResourceNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        }
    }

    /**
     * GET /api/users/count
     * Obtiene el total de usuarios
     */
    @GetMapping("/count")
    fun countUsers(): ResponseEntity<Map<String, Long>> {
        val count = userService.countUsers()
        return ResponseEntity.ok(mapOf("total" to count))
    }

    /**
     * GET /api/users/role/{role}
     * Obtiene usuarios por rol (USER, BUSINESS, ADMIN)
     */
    @GetMapping("/role/{role}")
    fun getUsersByRole(@PathVariable role: String): ResponseEntity<*> {
        return try {
            val roleEnum = Role.valueOf(role.uppercase())
            val users = userService.getUsersByRole(roleEnum)
            ResponseEntity.ok(users)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to "Rol inválido. Valores válidos: USER, BUSINESS, ADMIN"))
        }
    }

    /**
     * GET /api/users/admins-businesses
     * Obtiene todos los administradores y negocios
     */
    @GetMapping("/admins-businesses")
    fun getAdminsAndBusinesses(): ResponseEntity<List<UserDTO>> {
        val users = userService.getAdminsAndBusinesses()
        return ResponseEntity.ok(users)
    }
}
