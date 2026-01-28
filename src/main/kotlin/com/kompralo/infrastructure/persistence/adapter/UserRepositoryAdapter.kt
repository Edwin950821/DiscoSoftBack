package com.kompralo.infrastructure.persistence.adapter

import com.kompralo.domain.auth.model.User
import com.kompralo.domain.auth.repository.UserRepository
import com.kompralo.domain.auth.valueobject.Email
import com.kompralo.infrastructure.persistence.mapper.UserMapper
import com.kompralo.infrastructure.persistence.repository.JpaUserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val jpaRepository: JpaUserRepository,
    private val mapper: UserMapper
) : UserRepository {

    override fun findByEmail(email: Email): User? {
        return jpaRepository.findByEmail(email.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun existsByEmail(email: Email): Boolean {
        return jpaRepository.existsByEmail(email.value)
    }

    override fun save(user: User): User {
        val entity = mapper.toEntity(user)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findById(id: Long): User? {
        return jpaRepository.findById(id)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }
}
