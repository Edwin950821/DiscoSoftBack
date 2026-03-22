-- ==========================================
-- V5: Tablas de Billar (mesas + partidas)
-- ==========================================

CREATE TABLE IF NOT EXISTS disco_mesas_billar (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero          INT NOT NULL UNIQUE,
    nombre          VARCHAR(255) NOT NULL,
    precio_por_hora INT NOT NULL DEFAULT 20000,
    estado          VARCHAR(20) NOT NULL DEFAULT 'LIBRE',
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW()
);

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
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_partidas_billar_mesa ON disco_partidas_billar(mesa_billar_id);
CREATE INDEX IF NOT EXISTS idx_partidas_billar_fecha ON disco_partidas_billar(jornada_fecha);
CREATE INDEX IF NOT EXISTS idx_partidas_billar_estado ON disco_partidas_billar(estado);
