package com.kompralo.repository

import com.kompralo.model.DiscoInventario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DiscoInventarioRepository : JpaRepository<DiscoInventario, UUID> {

    fun findAllByOrderByCreadoEnDesc(): List<DiscoInventario>
}
