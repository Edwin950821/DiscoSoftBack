package com.kompralo.repository

import com.kompralo.model.DiscoJornada
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoJornadaRepository : JpaRepository<DiscoJornada, UUID> {

    fun findAllByOrderByCreadoEnDesc(): List<DiscoJornada>

    fun findByNegocioIdOrderByCreadoEnDesc(negocioId: UUID): List<DiscoJornada>
}
