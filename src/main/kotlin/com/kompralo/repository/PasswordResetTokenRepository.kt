package com.kompralo.repository

import com.kompralo.model.PasswordResetToken
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {

    fun findByToken(token: String): Optional<PasswordResetToken>

    fun findByUser(user: User): List<PasswordResetToken>

    fun deleteByUser(user: User)
}
