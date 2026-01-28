package com.kompralo.domain.auth.repository

import com.kompralo.domain.auth.model.User
import com.kompralo.domain.auth.valueobject.Email

interface UserRepository {
    fun findByEmail(email: Email): User?
    fun existsByEmail(email: Email): Boolean
    fun save(user: User): User
    fun findById(id: Long): User?
}
