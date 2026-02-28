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
}
