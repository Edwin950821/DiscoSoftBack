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
    fun migrateBillarTables() {
        try {
            // Crear tablas si no existen
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS disco_mesas_billar (
                    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    numero          INT NOT NULL,
                    nombre          VARCHAR(255) NOT NULL,
                    precio_por_hora INT NOT NULL DEFAULT 20000,
                    estado          VARCHAR(20) NOT NULL DEFAULT 'LIBRE',
                    activo          BOOLEAN NOT NULL DEFAULT TRUE,
                    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
                    negocio_id      UUID
                )
            """.trimIndent())

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS disco_partidas_billar (
                    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    mesa_billar_id  UUID NOT NULL REFERENCES disco_mesas_billar(id),
                    nombre_cliente  VARCHAR(255) DEFAULT 'Cliente',
                    hora_inicio     TIMESTAMP NOT NULL DEFAULT NOW(),
                    hora_fin        TIMESTAMP,
                    precio_por_hora INT NOT NULL,
                    horas_cobradas  INT,
                    total           INT,
                    estado          VARCHAR(20) NOT NULL DEFAULT 'EN_JUEGO',
                    jornada_fecha   VARCHAR(10) NOT NULL,
                    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
                    negocio_id      UUID
                )
            """.trimIndent())

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS disco_jornada_diaria (
                    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    fecha             VARCHAR(10) NOT NULL,
                    total_ventas      INT NOT NULL DEFAULT 0,
                    total_billar      INT NOT NULL DEFAULT 0,
                    total_general     INT NOT NULL DEFAULT 0,
                    cuentas_cerradas  INT NOT NULL DEFAULT 0,
                    tickets_totales   INT NOT NULL DEFAULT 0,
                    mesas_atendidas   INT NOT NULL DEFAULT 0,
                    partidas_billar   INT NOT NULL DEFAULT 0,
                    cerrado_en        TIMESTAMP NOT NULL DEFAULT NOW(),
                    negocio_id        UUID
                )
            """.trimIndent())

            // Eliminar constraint viejo UNIQUE(numero) si existe
            jdbcTemplate.execute("ALTER TABLE disco_mesas_billar DROP CONSTRAINT IF EXISTS disco_mesas_billar_numero_key")

            // Agregar negocio_id si falta (tablas creadas por V5 sin ella)
            val cols = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'disco_mesas_billar' AND column_name = 'negocio_id'"
            )
            if (cols.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE disco_mesas_billar ADD COLUMN negocio_id UUID")
                jdbcTemplate.execute("ALTER TABLE disco_partidas_billar ADD COLUMN negocio_id UUID")
            }

            // Indexes
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_partidas_billar_mesa ON disco_partidas_billar(mesa_billar_id)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_partidas_billar_fecha ON disco_partidas_billar(jornada_fecha)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_partidas_billar_estado ON disco_partidas_billar(estado)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_mesas_billar_negocio ON disco_mesas_billar(negocio_id)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_partidas_billar_negocio ON disco_partidas_billar(negocio_id)")

            log.info("Billar tables ready")
        } catch (e: Exception) {
            log.error("Billar migration error: ${e.message}", e)
        }
    }
}
