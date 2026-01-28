package com.kompralo.infrastructure.persistence.mapper

import com.kompralo.domain.auth.model.User
import com.kompralo.domain.auth.valueobject.Email
import com.kompralo.domain.auth.valueobject.Password
import com.kompralo.domain.auth.valueobject.Role
import com.kompralo.infrastructure.persistence.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id,
            email = Email(entity.email),
            password = Password.fromHashed(entity.password),
            name = entity.name,
            role = Role.fromString(entity.role),
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: User): UserEntity {
        return UserEntity(
            id = domain.id,
            email = domain.email.value,
            password = domain.password.value,
            name = domain.name,
            role = domain.role.name,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}