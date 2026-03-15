package com.kompralo.controller

import com.kompralo.dto.CreateUserRequest
import com.kompralo.dto.PagedResponse
import com.kompralo.dto.UpdateUserRequest
import com.kompralo.dto.UserDTO
import com.kompralo.model.Role
import com.kompralo.services.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

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

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserDTO> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user)
    }

    @GetMapping("/email/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<UserDTO> {
        val user = userService.getUserByEmail(email)
        return ResponseEntity.ok(user)
    }

    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserDTO> {
        val user = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<UserDTO> {
        val user = userService.updateUser(id, request)
        return ResponseEntity.ok(user)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        userService.deleteUser(id)
        return ResponseEntity.ok(mapOf("message" to "Usuario eliminado exitosamente"))
    }

    @GetMapping("/count")
    fun countUsers(): ResponseEntity<Map<String, Long>> {
        val count = userService.countUsers()
        return ResponseEntity.ok(mapOf("total" to count))
    }

    @GetMapping("/role/{role}")
    fun getUsersByRole(@PathVariable role: String): ResponseEntity<List<UserDTO>> {
        val roleEnum = Role.valueOf(role.uppercase())
        val users = userService.getUsersByRole(roleEnum)
        return ResponseEntity.ok(users)
    }

    @GetMapping("/admins-businesses")
    fun getAdminsAndBusinesses(): ResponseEntity<List<UserDTO>> {
        val users = userService.getAdminsAndBusinesses()
        return ResponseEntity.ok(users)
    }
}
