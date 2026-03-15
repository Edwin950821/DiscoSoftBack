package com.kompralo.repository

import com.kompralo.model.DiscoMesa
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoMesaRepository : JpaRepository<DiscoMesa, UUID> {

    fun findAllByOrderByNumeroAsc(): List<DiscoMesa>

    fun findByEstado(estado: String): List<DiscoMesa>

    fun findByMeseroId(meseroId: UUID): List<DiscoMesa>

    fun findByNumero(numero: Int): DiscoMesa?
}
