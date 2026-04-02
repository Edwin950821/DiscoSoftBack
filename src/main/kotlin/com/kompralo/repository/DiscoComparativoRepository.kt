package com.kompralo.repository

import com.kompralo.model.DiscoComparativo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoComparativoRepository : JpaRepository<DiscoComparativo, UUID> {
    fun findAllByOrderByCreadoEnDesc(): List<DiscoComparativo>

    fun findByNegocioIdOrderByCreadoEnDesc(negocioId: UUID): List<DiscoComparativo>
}
