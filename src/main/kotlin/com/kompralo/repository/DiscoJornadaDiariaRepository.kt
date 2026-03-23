package com.kompralo.repository

import com.kompralo.model.DiscoJornadaDiaria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoJornadaDiariaRepository : JpaRepository<DiscoJornadaDiaria, UUID> {
    fun findByFecha(fecha: String): DiscoJornadaDiaria?
    fun findAllByOrderByCerradoEnDesc(): List<DiscoJornadaDiaria>

    fun findByNegocioIdAndFecha(negocioId: UUID, fecha: String): DiscoJornadaDiaria?
    fun findByNegocioIdOrderByCerradoEnDesc(negocioId: UUID): List<DiscoJornadaDiaria>
}
