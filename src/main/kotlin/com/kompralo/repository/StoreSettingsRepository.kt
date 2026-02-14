package com.kompralo.repository

import com.kompralo.model.StoreSettings
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface StoreSettingsRepository : JpaRepository<StoreSettings, Long> {
    fun findBySeller(seller: User): Optional<StoreSettings>
    fun existsBySeller(seller: User): Boolean
}
