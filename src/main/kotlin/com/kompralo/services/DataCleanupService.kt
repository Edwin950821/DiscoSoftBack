package com.kompralo.services

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DataCleanupService(
    private val txManager: PlatformTransactionManager
) {

    private val log = LoggerFactory.getLogger(DataCleanupService::class.java)

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @EventListener(ApplicationReadyEvent::class)
    fun limpiarAlIniciar() {
        try {
            migrarNegocioId()
            limpiarDatosViejos()
        } catch (e: Exception) {
            log.error("Error durante tareas automaticas al iniciar: ${e.message}", e)
        }
    }

    /**
     * Asigna negocio_id a filas existentes que lo tengan NULL.
     * Usa el primer negocio encontrado. Excluye tablas de billar.
     */
    private fun migrarNegocioId() {
        val tablas = listOf(
            "disco_mesas",
            "disco_meseros",
            "disco_productos",
            "disco_pedidos",
            "disco_linea_pedido",
            "disco_cuenta_mesa",
            "disco_promociones",
            "disco_inventarios",
            "disco_linea_inventario",
            "disco_jornadas",
            "disco_mesero_jornada",
            "disco_jornada_diaria",
            "disco_comparativos",
            "disco_linea_comparativo"
        )

        try {

            val negocioId = TransactionTemplate(txManager).execute {
                @Suppress("UNCHECKED_CAST")
                val resultado = entityManager.createNativeQuery(
                    "SELECT negocio_id FROM auth_users WHERE negocio_id IS NOT NULL LIMIT 1"
                ).resultList as List<Any>
                if (resultado.isEmpty()) null else resultado[0]
            }

            if (negocioId == null) {
                log.info("=== MIGRACION negocio_id: no hay usuarios con negocio, saltando ===")
                return
            }

            var totalActualizado = 0

            for (tabla in tablas) {
                try {
                    val updated = TransactionTemplate(txManager).execute {
                        entityManager.createNativeQuery(
                            "UPDATE $tabla SET negocio_id = :negocioId WHERE negocio_id IS NULL"
                        ).setParameter("negocioId", negocioId).executeUpdate()
                    } ?: 0

                    if (updated > 0) {
                        log.info("  -> $tabla: $updated filas actualizadas con negocio_id")
                        totalActualizado += updated
                    }
                } catch (e: Exception) {
                    log.debug("Tabla $tabla no encontrada o sin columna negocio_id: ${e.message}")
                }
            }

            if (totalActualizado > 0) {
                log.info("=== MIGRACION negocio_id COMPLETADA: $totalActualizado filas actualizadas ===")
            } else {
                log.info("=== MIGRACION negocio_id: no habia filas sin negocio_id ===")
            }
        } catch (e: Exception) {
            log.error("Error durante migracion de negocio_id: ${e.message}", e)
        }
    }

    fun limpiarDatosViejos() {
        val fechaLimite = LocalDateTime.now().minusMonths(12)
        val fechaLimiteStr = fechaLimite.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        log.info("=== LIMPIEZA AUTOMATICA: eliminando datos anteriores a $fechaLimiteStr ===")

        val queries = listOf(

            "DELETE FROM disco_linea_pedido WHERE pedido_id IN (SELECT id FROM disco_pedidos WHERE creado_en < :fecha)",
            "DELETE FROM disco_pedidos WHERE creado_en < :fecha",
            "DELETE FROM disco_cuenta_mesa WHERE creado_en < :fecha",
            "DELETE FROM disco_partidas_billar WHERE creado_en < :fecha",
            "DELETE FROM disco_mesero_jornada WHERE jornada_id IN (SELECT id FROM disco_jornadas WHERE creado_en < :fecha)",
            "DELETE FROM disco_jornadas WHERE creado_en < :fecha",
            "DELETE FROM disco_jornada_diaria WHERE cerrado_en < :fecha",
            "DELETE FROM disco_linea_inventario WHERE inventario_id IN (SELECT id FROM disco_inventarios WHERE creado_en < :fecha)",
            "DELETE FROM disco_inventarios WHERE creado_en < :fecha",
            "DELETE FROM disco_linea_comparativo WHERE comparativo_id IN (SELECT id FROM disco_comparativos WHERE creado_en < :fecha)",
            "DELETE FROM disco_comparativos WHERE creado_en < :fecha",
            "DELETE FROM analytics_events WHERE created_at < :fecha",
            "DELETE FROM email_delivery_log WHERE sent_at < :fecha",
            "DELETE FROM notifications WHERE created_at < :fecha",
            "DELETE FROM password_reset_tokens WHERE created_at < :fecha"
        )

        var totalEliminado = 0

        for (sql in queries) {
            totalEliminado += ejecutarEnTransaccion(sql, fechaLimite)
        }

        if (totalEliminado > 0) {
            log.info("=== LIMPIEZA COMPLETADA: $totalEliminado registros eliminados ===")
        } else {
            log.info("=== LIMPIEZA: no habia datos anteriores a 12 meses ===")
        }
    }

    private fun ejecutarEnTransaccion(sql: String, fechaLimite: LocalDateTime): Int {
        return try {
            TransactionTemplate(txManager).execute {
                entityManager.createNativeQuery(sql)
                    .setParameter("fecha", fechaLimite)
                    .executeUpdate()
            } ?: 0
        } catch (e: Exception) {
            log.debug("Tabla no encontrada o error: ${e.message}")
            0
        }
    }
}
