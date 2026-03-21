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
            limpiarDatosViejos()
        } catch (e: Exception) {
            log.error("Error durante limpieza automatica de datos: ${e.message}", e)
        }
    }

    fun limpiarDatosViejos() {
        val fechaLimite = LocalDateTime.now().minusMonths(12)
        val fechaLimiteStr = fechaLimite.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        log.info("=== LIMPIEZA AUTOMATICA: eliminando datos anteriores a $fechaLimiteStr ===")

        val queries = listOf(
            // DISCO: Pedidos y Cuentas (orden correcto por FK)
            "DELETE FROM disco_linea_pedido WHERE pedido_id IN (SELECT id FROM disco_pedidos WHERE creado_en < :fecha)",
            "DELETE FROM disco_pedidos WHERE creado_en < :fecha",
            "DELETE FROM disco_cuenta_mesa WHERE creado_en < :fecha",
            // DISCO: Billar
            "DELETE FROM disco_billar_partidas WHERE creado_en < :fecha",
            // DISCO: Jornadas
            "DELETE FROM disco_mesero_jornada WHERE jornada_id IN (SELECT id FROM disco_jornadas WHERE creado_en < :fecha)",
            "DELETE FROM disco_jornadas WHERE creado_en < :fecha",
            "DELETE FROM disco_resumen_jornada WHERE creado_en < :fecha",
            // DISCO: Inventarios
            "DELETE FROM disco_linea_inventario WHERE inventario_id IN (SELECT id FROM disco_inventarios WHERE creado_en < :fecha)",
            "DELETE FROM disco_inventarios WHERE creado_en < :fecha",
            // DISCO: Comparativos
            "DELETE FROM disco_linea_comparativo WHERE comparativo_id IN (SELECT id FROM disco_comparativos WHERE creado_en < :fecha)",
            "DELETE FROM disco_comparativos WHERE creado_en < :fecha",
            // GENERAL: Logs y notificaciones
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
