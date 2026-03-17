package com.kompralo.repository

import com.kompralo.model.DiscoPromocion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoPromocionRepository : JpaRepository<DiscoPromocion, UUID> {
    fun findAllByOrderByCreadoEnDesc(): List<DiscoPromocion>
    fun findByActivaTrue(): List<DiscoPromocion>
}
