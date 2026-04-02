package com.kompralo.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DatabaseMigration(
    private val jdbcTemplate: JdbcTemplate
) {
    private val log = LoggerFactory.getLogger(DatabaseMigration::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun fixOrdersForeignKeys() {
        try {
            val badFks = jdbcTemplate.queryForList(
                """
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
                WHERE tc.table_name = 'orders'
                  AND tc.constraint_type = 'FOREIGN KEY'
                  AND ccu.table_name = 'users'
                """.trimIndent()
            )

            if (badFks.isEmpty()) {
                return
            }

            log.warn("Found ${badFks.size} FK(s) on 'orders' pointing to 'users' instead of 'auth_users'. Fixing...")

            for (fk in badFks) {
                val constraintName = fk["constraint_name"] as String
                jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT $constraintName")
                log.info("Dropped bad FK: $constraintName")
            }

            jdbcTemplate.execute("ALTER TABLE orders ADD CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES auth_users(id)")
            jdbcTemplate.execute("ALTER TABLE orders ADD CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES auth_users(id)")
            log.info("Recreated FKs pointing to auth_users")
        } catch (e: Exception) {
            log.error("FK migration skipped: ${e.message}")
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun migrateBillarNegocioId() {
        try {
            val tableExists = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_name = 'disco_mesas_billar'"
            )
            if (tableExists.isEmpty()) return // tabla no existe aun, ddl-auto la creara bien

            // Siempre intentar eliminar el constraint viejo UNIQUE(numero) que bloquea inserts
            jdbcTemplate.execute("ALTER TABLE disco_mesas_billar DROP CONSTRAINT IF EXISTS disco_mesas_billar_numero_key")

            // Si la columna negocio_id no existe, agregarla manualmente
            val cols = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'disco_mesas_billar' AND column_name = 'negocio_id'"
            )
            if (cols.isEmpty()) {
                log.warn("Adding negocio_id to billar tables...")
                jdbcTemplate.execute("ALTER TABLE disco_mesas_billar ADD COLUMN negocio_id UUID")
                jdbcTemplate.execute("ALTER TABLE disco_partidas_billar ADD COLUMN negocio_id UUID")
            }

            // Asignar negocio_id a filas huerfanas
            jdbcTemplate.execute("""
                UPDATE disco_mesas_billar SET negocio_id = COALESCE(
                    (SELECT negocio_id FROM auth_users WHERE negocio_id IS NOT NULL LIMIT 1),
                    '00000000-0000-0000-0000-000000000000'::uuid
                ) WHERE negocio_id IS NULL
            """.trimIndent())
            jdbcTemplate.execute("""
                UPDATE disco_partidas_billar SET negocio_id = COALESCE(
                    (SELECT negocio_id FROM auth_users WHERE negocio_id IS NOT NULL LIMIT 1),
                    '00000000-0000-0000-0000-000000000000'::uuid
                ) WHERE negocio_id IS NULL
            """.trimIndent())

            // Indexes
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_mesas_billar_negocio ON disco_mesas_billar(negocio_id)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_partidas_billar_negocio ON disco_partidas_billar(negocio_id)")

            log.info("Billar migration check complete")
        } catch (e: Exception) {
            log.error("Billar migration skipped: ${e.message}")
        }
    }
}
