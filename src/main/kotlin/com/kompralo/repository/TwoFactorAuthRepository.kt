package com.kompralo.repository

import com.kompralo.model.TwoFactorAuth
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TwoFactorAuthRepository : JpaRepository<TwoFactorAuth, Long> {

    fun findByUser(user: User): Optional<TwoFactorAuth>

    fun existsByUser(user: User): Boolean

    fun findByUserAndEnabledTrue(user: User): Optional<TwoFactorAuth>

    fun deleteByUser(user: User)
}
