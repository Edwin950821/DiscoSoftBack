package com.kompralo.services

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DataCleanupService {

    private val log = LoggerFactory.getLogger(DataCleanupService::class.java)

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * Se ejecuta cada vez que el servidor arranca.
     * En Render free, el server se duerme por inactividad y se despierta
     * cuando alguien hace un request. Al despertar, revisa si hay datos
     * con mas de 12 meses y los borra.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun limpiarAlIniciar() {
        try {
            limpiarDatosViejos()
        } catch (e: Exception) {
            log.error("Error durante limpieza automatica de datos: ${e.message}", e)
        }
    }

    @Transactional
    fun limpiarDatosViejos() {
        val fechaLimite = LocalDateTime.now().minusMonths(12)
        val fechaLimiteStr = fechaLimite.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        log.info("=== LIMPIEZA AUTOMATICA: eliminando datos anteriores a $fechaLimiteStr ===")

        var totalEliminado = 0

        // ─── DISCO: Pedidos y Cuentas (orden correcto por FK) ───

        // 1. Lineas de pedido de pedidos viejos
        val lineasPedido = entityManager.createNativeQuery(
            """DELETE FROM disco_linea_pedido
               WHERE pedido_id IN (SELECT id FROM disco_pedidos WHERE creado_en < :fecha)"""
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += lineasPedido

        // 2. Pedidos viejos
        val pedidos = entityManager.createNativeQuery(
            "DELETE FROM disco_pedidos WHERE creado_en < :fecha"
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += pedidos

        // 3. Cuentas de mesa viejas
        val cuentas = entityManager.createNativeQuery(
            "DELETE FROM disco_cuenta_mesa WHERE creado_en < :fecha"
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += cuentas

        // ─── DISCO: Billar ───

        // 4. Partidas de billar viejas
        val partidas = entityManager.createNativeQuery(
            "DELETE FROM disco_billar_partidas WHERE creado_en < :fecha"
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += partidas

        // ─── DISCO: Jornadas ───

        // 5. Meseros de jornada viejos (FK a disco_jornadas)
        val meserosJornada = entityManager.createNativeQuery(
            """DELETE FROM disco_mesero_jornada
               WHERE jornada_id IN (SELECT id FROM disco_jornadas WHERE creado_en < :fecha)"""
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += meserosJornada

        // 6. Jornadas viejas
        val jornadas = entityManager.createNativeQuery(
            "DELETE FROM disco_jornadas WHERE creado_en < :fecha"
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += jornadas

        // 7. Resumen de jornadas diarias viejas
        val resumenes = entityManager.createNativeQuery(
            "DELETE FROM disco_resumen_jornada WHERE creado_en < :fecha"
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += resumenes

        // ─── DISCO: Inventarios ───

        // 8. Lineas de inventario viejas
        val lineasInv = entityManager.createNativeQuery(
            """DELETE FROM disco_linea_inventario
               WHERE inventario_id IN (SELECT id FROM disco_inventarios WHERE creado_en < :fecha)"""
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += lineasInv

        // 9. Inventarios viejos
        val inventarios = entityManager.createNativeQuery(
            "DELETE FROM disco_inventarios WHERE creado_en < :fecha"
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += inventarios

        // ─── DISCO: Comparativos ───

        // 10. Lineas de comparativo viejas
        val lineasComp = entityManager.createNativeQuery(
            """DELETE FROM disco_linea_comparativo
               WHERE comparativo_id IN (SELECT id FROM disco_comparativos WHERE creado_en < :fecha)"""
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += lineasComp

        // 11. Comparativos viejos
        val comparativos = entityManager.createNativeQuery(
            "DELETE FROM disco_comparativos WHERE creado_en < :fecha"
        ).setParameter("fecha", fechaLimite).executeUpdate()
        totalEliminado += comparativos

        // ─── GENERAL: Logs y notificaciones ───

        // 12. Analytics events viejos
        val analytics = ejecutarSiExiste(
            "DELETE FROM analytics_events WHERE created_at < :fecha", fechaLimite
        )
        totalEliminado += analytics

        // 13. Email delivery logs viejos
        val emails = ejecutarSiExiste(
            "DELETE FROM email_delivery_log WHERE sent_at < :fecha", fechaLimite
        )
        totalEliminado += emails

        // 14. Notificaciones viejas
        val notificaciones = ejecutarSiExiste(
            "DELETE FROM notifications WHERE created_at < :fecha", fechaLimite
        )
        totalEliminado += notificaciones

        // 15. Password reset tokens viejos
        val tokens = ejecutarSiExiste(
            "DELETE FROM password_reset_tokens WHERE created_at < :fecha", fechaLimite
        )
        totalEliminado += tokens

        if (totalEliminado > 0) {
            log.info("=== LIMPIEZA COMPLETADA: $totalEliminado registros eliminados ===")
        } else {
            log.info("=== LIMPIEZA: no habia datos anteriores a 12 meses ===")
        }
    }

    /**
     * Ejecuta un DELETE y si la tabla no existe (porque aun no se creo),
     * simplemente lo ignora y retorna 0.
     */
    private fun ejecutarSiExiste(sql: String, fechaLimite: LocalDateTime): Int {
        return try {
            entityManager.createNativeQuery(sql)
                .setParameter("fecha", fechaLimite)
                .executeUpdate()
        } catch (e: Exception) {
            log.debug("Tabla no encontrada o error en: $sql - ${e.message}")
            0
        }
    }
}
