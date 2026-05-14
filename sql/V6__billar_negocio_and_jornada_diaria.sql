-- ==========================================
-- V6: Agregar negocio_id a tablas de billar
--     + Crear tabla disco_jornada_diaria
-- ==========================================

-- 1. Agregar negocio_id a disco_mesas_billar
ALTER TABLE disco_mesas_billar ADD COLUMN IF NOT EXISTS negocio_id UUID;

-- Asignar negocio_id a registros existentes (tomar del primer usuario con negocio)
UPDATE disco_mesas_billar SET negocio_id = COALESCE(
    (SELECT negocio_id FROM auth_users WHERE negocio_id IS NOT NULL LIMIT 1),
    '00000000-0000-0000-0000-000000000000'::uuid
) WHERE negocio_id IS NULL;

-- Hacer NOT NULL despues de rellenar
ALTER TABLE disco_mesas_billar ALTER COLUMN negocio_id SET NOT NULL;

-- Reemplazar constraint UNIQUE de solo numero por (numero, negocio_id)
ALTER TABLE disco_mesas_billar DROP CONSTRAINT IF EXISTS disco_mesas_billar_numero_key;
ALTER TABLE disco_mesas_billar ADD CONSTRAINT uq_mesa_billar_numero_negocio UNIQUE (numero, negocio_id);

-- 2. Agregar negocio_id a disco_partidas_billar
ALTER TABLE disco_partidas_billar ADD COLUMN IF NOT EXISTS negocio_id UUID;

UPDATE disco_partidas_billar SET negocio_id = COALESCE(
    (SELECT negocio_id FROM auth_users WHERE negocio_id IS NOT NULL LIMIT 1),
    '00000000-0000-0000-0000-000000000000'::uuid
) WHERE negocio_id IS NULL;

ALTER TABLE disco_partidas_billar ALTER COLUMN negocio_id SET NOT NULL;

-- Indice para consultas por negocio
CREATE INDEX IF NOT EXISTS idx_partidas_billar_negocio ON disco_partidas_billar(negocio_id);
CREATE INDEX IF NOT EXISTS idx_mesas_billar_negocio ON disco_mesas_billar(negocio_id);

-- 3. Crear tabla disco_jornada_diaria
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
    negocio_id        UUID NOT NULL,
    CONSTRAINT uq_jornada_diaria_fecha_negocio UNIQUE (fecha, negocio_id)
);

CREATE INDEX IF NOT EXISTS idx_jornada_diaria_negocio ON disco_jornada_diaria(negocio_id);
