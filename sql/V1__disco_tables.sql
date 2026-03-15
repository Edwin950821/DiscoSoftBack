-- =============================================
-- Monastery Club - Tablas de gestión
-- Ejecutar en la BD: monastery
-- =============================================

-- 1. Productos
CREATE TABLE disco_productos (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    precio      INTEGER      NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. Meseros
CREATE TABLE disco_meseros (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    color       VARCHAR(20)  NOT NULL,
    avatar      VARCHAR(5)   NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 3. Jornadas (liquidación diaria)
CREATE TABLE disco_jornadas (
    id              BIGSERIAL PRIMARY KEY,
    sesion          VARCHAR(50)  NOT NULL,
    fecha           VARCHAR(10)  NOT NULL,
    total_vendido   INTEGER      NOT NULL DEFAULT 0,
    total_recibido  INTEGER      NOT NULL DEFAULT 0,
    saldo           INTEGER      NOT NULL DEFAULT 0,
    cortesias       INTEGER      NOT NULL DEFAULT 0,
    gastos          INTEGER      NOT NULL DEFAULT 0,
    pagos_efectivo  INTEGER      NOT NULL DEFAULT 0,
    pagos_qr        INTEGER      NOT NULL DEFAULT 0,
    pagos_nequi     INTEGER      NOT NULL DEFAULT 0,
    pagos_datafono  INTEGER      NOT NULL DEFAULT 0,
    pagos_vales     INTEGER      NOT NULL DEFAULT 0,
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 4. Mesero por jornada (detalle por mesero)
CREATE TABLE disco_mesero_jornada (
    id              BIGSERIAL PRIMARY KEY,
    jornada_id      BIGINT       NOT NULL REFERENCES disco_jornadas(id) ON DELETE CASCADE,
    mesero_id       BIGINT       NOT NULL,
    nombre          VARCHAR(255) NOT NULL,
    color           VARCHAR(20)  NOT NULL,
    avatar          VARCHAR(5)   NOT NULL,
    total_mesero    INTEGER      NOT NULL DEFAULT 0,
    cortesias       INTEGER      NOT NULL DEFAULT 0,
    gastos          INTEGER      NOT NULL DEFAULT 0,
    pagos_efectivo  INTEGER      NOT NULL DEFAULT 0,
    pagos_qr        INTEGER      NOT NULL DEFAULT 0,
    pagos_nequi     INTEGER      NOT NULL DEFAULT 0,
    pagos_datafono  INTEGER      NOT NULL DEFAULT 0,
    pagos_vales     INTEGER      NOT NULL DEFAULT 0
);

-- 5. Inventarios (liquidación de inventario)
CREATE TABLE disco_inventarios (
    id              BIGSERIAL PRIMARY KEY,
    fecha           VARCHAR(10)  NOT NULL,
    total_general   INTEGER      NOT NULL DEFAULT 0,
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 6. Líneas de inventario (detalle por producto)
CREATE TABLE disco_linea_inventario (
    id              BIGSERIAL PRIMARY KEY,
    inventario_id   BIGINT       NOT NULL REFERENCES disco_inventarios(id) ON DELETE CASCADE,
    producto_id     BIGINT       NOT NULL,
    nombre          VARCHAR(255) NOT NULL,
    valor_unitario  INTEGER      NOT NULL,
    inv_inicial     INTEGER      NOT NULL DEFAULT 0,
    entradas        INTEGER      NOT NULL DEFAULT 0,
    inv_fisico      INTEGER      NOT NULL DEFAULT 0,
    saldo           INTEGER      NOT NULL DEFAULT 0,
    total           INTEGER      NOT NULL DEFAULT 0
);

-- Índices
CREATE INDEX idx_disco_jornadas_fecha ON disco_jornadas(fecha);
CREATE INDEX idx_disco_jornadas_creado ON disco_jornadas(creado_en DESC);
CREATE INDEX idx_disco_mesero_jornada_jornada ON disco_mesero_jornada(jornada_id);
CREATE INDEX idx_disco_inventarios_fecha ON disco_inventarios(fecha);
CREATE INDEX idx_disco_inventarios_creado ON disco_inventarios(creado_en DESC);
CREATE INDEX idx_disco_linea_inv_inventario ON disco_linea_inventario(inventario_id);

-- Datos iniciales: Productos
INSERT INTO disco_productos (nombre, precio) VALUES
    ('Aguila Negra 330ml', 5000),
    ('Aguila Light', 5000),
    ('Costeñita 330ml', 5000),
    ('Coronita 355ml', 6000),
    ('Club Colombia', 6000),
    ('Heineken', 6000),
    ('Budweiser', 6000),
    ('Stella Artois', 10000),
    ('Smirnoff Ice', 14000),
    ('Antioqueño Litro Tapa Verde', 150000),
    ('Antioqueño 750ml Verde', 120000),
    ('Antioqueño Litro Amarillo', 150000),
    ('Old Parr 1 Litro', 280000),
    ('Agua', 5000),
    ('Coca Cola', 5000),
    ('Soda', 5000),
    ('Gatorade', 7000),
    ('Electrolit', 12000),
    ('Redbull', 15000),
    ('Vaso Michelado', 3000),
    ('Bombon', 1000),
    ('Detodito', 7000),
    ('Mani', 3000);

-- Datos iniciales: Meseros
INSERT INTO disco_meseros (nombre, color, avatar) VALUES
    ('Gabriel', '#FF6B35', 'GA'),
    ('Carlos',  '#4ECDC4', 'CA'),
    ('Loraine', '#FFE66D', 'LO'),
    ('Luis',    '#A8E6CF', 'LU'),
    ('Barra',   '#C3B1E1', 'BA');
