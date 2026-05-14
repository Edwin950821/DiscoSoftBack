package com.kompralo.repository

import com.kompralo.model.User
import com.kompralo.model.UserProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserProfileRepository : JpaRepository<UserProfile, Long> {

    fun findByUser(user: User): Optional<UserProfile>

    fun existsByUser(user: User): Boolean
}
