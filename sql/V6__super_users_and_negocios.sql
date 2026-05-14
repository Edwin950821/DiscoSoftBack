-- ==========================================
-- V6: Super usuarios + negocio Monastery
-- ==========================================
-- Crea 3 super usuarios capaces de ver el dashboard consolidado
-- de todos los negocios activos. Asegura también que Monastery Club
-- exista en `negocios` (los demás negocios se agregan manualmente
-- después).
--
-- IMPORTANTE: Ejecutar DESPUÉS de levantar la app al menos una vez
-- para que Hibernate cree las tablas `negocios` y `auth_users` con
-- todas las columnas (incluyendo `negocio_id`, `created_at`, etc.)
--
-- Password de los 3 super usuarios: Super2026!
-- (hashed in-place via pgcrypto, compatible con BCryptPasswordEncoder)
-- ==========================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─── 1. Asegurar negocio Monastery Club ───
-- Los negocios adicionales se agregan manualmente después.

INSERT INTO negocios (id, nombre, slug, color_primario, activo, creado_en)
VALUES
    (gen_random_uuid(), 'Monastery Club', 'monastery', '#D4AF37', true, NOW())
ON CONFLICT (slug) DO NOTHING;

-- ─── 2. Crear 3 super usuarios (ven todos los negocios) ───
-- negocio_id = NULL porque los SUPER no están atados a un negocio.
-- created_at / updated_at son NOT NULL en la entidad → se proveen explícitamente.

INSERT INTO auth_users (
    email, username, password, name, role, is_active, negocio_id,
    uuid, created_at, updated_at, failed_login_attempts
) VALUES
    ('super1@monastery.co', 'super1',
     crypt('Super2026!', gen_salt('bf', 10)),
     'Super Usuario 1', 'SUPER', true, NULL,
     gen_random_uuid()::text, NOW(), NOW(), 0),
    ('super2@monastery.co', 'super2',
     crypt('Super2026!', gen_salt('bf', 10)),
     'Super Usuario 2', 'SUPER', true, NULL,
     gen_random_uuid()::text, NOW(), NOW(), 0),
    ('super3@monastery.co', 'super3',
     crypt('Super2026!', gen_salt('bf', 10)),
     'Super Usuario 3', 'SUPER', true, NULL,
     gen_random_uuid()::text, NOW(), NOW(), 0)
ON CONFLICT (email) DO NOTHING;
