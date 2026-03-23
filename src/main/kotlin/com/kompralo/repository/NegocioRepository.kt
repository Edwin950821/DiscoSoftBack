package com.kompralo.repository

import com.kompralo.model.Negocio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NegocioRepository : JpaRepository<Negocio, UUID> {
    fun findBySlug(slug: String): Negocio?
    fun findByActivoTrue(): List<Negocio>
}
