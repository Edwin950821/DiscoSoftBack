package com.kompralo.repository

import com.kompralo.model.DiscoProducto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoProductoRepository : JpaRepository<DiscoProducto, UUID> {

    fun findAllByOrderByCreadoEnDesc(): List<DiscoProducto>

    fun findByActivoTrue(): List<DiscoProducto>
}
