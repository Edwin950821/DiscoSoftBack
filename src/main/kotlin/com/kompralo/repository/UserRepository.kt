package com.kompralo.repository

import com.kompralo.model.Role
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): Optional<User>

    fun findByUsername(username: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByUsername(username: String): Boolean

    fun findByRole(role: Role): List<User>

    fun findByRoleIn(roles: List<Role>): List<User>
}
