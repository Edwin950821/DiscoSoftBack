package com.kompralo.repository

import com.kompralo.model.DiscoMesero
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoMeseroRepository : JpaRepository<DiscoMesero, UUID> {

    fun findAllByOrderByCreadoEnDesc(): List<DiscoMesero>

    fun findByActivoTrue(): List<DiscoMesero>

    fun findByUsername(username: String): DiscoMesero?

    fun findByNegocioIdOrderByCreadoEnDesc(negocioId: UUID): List<DiscoMesero>

    fun findByNegocioIdAndActivoTrue(negocioId: UUID): List<DiscoMesero>

    fun findByNegocioIdAndUsername(negocioId: UUID, username: String): DiscoMesero?
}
