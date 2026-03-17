package com.kompralo.repository

import com.kompralo.model.DiscoMesaBillar
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DiscoMesaBillarRepository : JpaRepository<DiscoMesaBillar, UUID> {
    fun findAllByOrderByNumeroAsc(): List<DiscoMesaBillar>
}
