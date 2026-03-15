-- =============================================
-- Monastery Club - V2: Migración a UUID + Sistema de Pedidos
-- IMPORTANTE: Ejecutar en la BD: monastery
-- NOTA: Esta migración DESTRUYE datos existentes. Respaldar antes.
-- =============================================

-- Habilitar extensión UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── 1. ELIMINAR TABLAS EXISTENTES (orden por dependencias) ───

DROP TABLE IF EXISTS disco_linea_inventario CASCADE;
DROP TABLE IF EXISTS disco_inventarios CASCADE;
DROP TABLE IF EXISTS disco_mesero_jornada CASCADE;
DROP TABLE IF EXISTS disco_jornadas CASCADE;
DROP TABLE IF EXISTS disco_meseros CASCADE;
DROP TABLE IF EXISTS disco_productos CASCADE;

-- ─── 2. RECREAR TABLAS EXISTENTES CON UUID ───

CREATE TABLE disco_productos (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    precio      INTEGER      NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE disco_meseros (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    color       VARCHAR(20)  NOT NULL,
    avatar      VARCHAR(5)   NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE disco_jornadas (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
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

CREATE TABLE disco_mesero_jornada (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    jornada_id      UUID         NOT NULL REFERENCES disco_jornadas(id) ON DELETE CASCADE,
    mesero_id       UUID         NOT NULL,
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

CREATE TABLE disco_inventarios (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    fecha           VARCHAR(10)  NOT NULL,
    total_general   INTEGER      NOT NULL DEFAULT 0,
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE disco_linea_inventario (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    inventario_id   UUID         NOT NULL REFERENCES disco_inventarios(id) ON DELETE CASCADE,
    producto_id     UUID         NOT NULL,
    nombre          VARCHAR(255) NOT NULL,
    valor_unitario  INTEGER      NOT NULL,
    inv_inicial     INTEGER      NOT NULL DEFAULT 0,
    entradas        INTEGER      NOT NULL DEFAULT 0,
    inv_fisico      INTEGER      NOT NULL DEFAULT 0,
    saldo           INTEGER      NOT NULL DEFAULT 0,
    total           INTEGER      NOT NULL DEFAULT 0
);

-- ─── 3. TABLAS NUEVAS: MESAS ───

CREATE TABLE disco_mesas (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    numero          INTEGER      NOT NULL UNIQUE,
    nombre          VARCHAR(50)  NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'LIBRE',
    mesero_id       UUID         NULL REFERENCES disco_meseros(id) ON SET NULL,
    jornada_id      UUID         NULL,
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── 4. TABLAS NUEVAS: PEDIDOS ───

CREATE TABLE disco_pedidos (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    mesa_id         UUID         NOT NULL REFERENCES disco_mesas(id),
    mesero_id       UUID         NOT NULL REFERENCES disco_meseros(id),
    ticket_dia      INTEGER      NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    total           INTEGER      NOT NULL DEFAULT 0,
    jornada_fecha   VARCHAR(10)  NOT NULL,
    nota            TEXT         NULL,
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW(),
    despachado_en   TIMESTAMP    NULL,
    cancelado_en    TIMESTAMP    NULL
);

CREATE TABLE disco_linea_pedido (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    pedido_id       UUID         NOT NULL REFERENCES disco_pedidos(id) ON DELETE CASCADE,
    producto_id     UUID         NOT NULL REFERENCES disco_productos(id),
    nombre          VARCHAR(255) NOT NULL,
    precio_unitario INTEGER      NOT NULL,
    cantidad        INTEGER      NOT NULL DEFAULT 1,
    total           INTEGER      NOT NULL DEFAULT 0
);

-- ─── 5. TABLA: CUENTA DE MESA ───

CREATE TABLE disco_cuenta_mesa (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    mesa_id         UUID         NOT NULL REFERENCES disco_mesas(id),
    mesero_id       UUID         NOT NULL REFERENCES disco_meseros(id),
    jornada_fecha   VARCHAR(10)  NOT NULL,
    total           INTEGER      NOT NULL DEFAULT 0,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA',
    pagada_en       TIMESTAMP    NULL,
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── 6. ÍNDICES ───

CREATE INDEX idx_disco_productos_creado ON disco_productos(creado_en DESC);
CREATE INDEX idx_disco_meseros_creado ON disco_meseros(creado_en DESC);
CREATE INDEX idx_disco_jornadas_fecha ON disco_jornadas(fecha);
CREATE INDEX idx_disco_jornadas_creado ON disco_jornadas(creado_en DESC);
CREATE INDEX idx_disco_mesero_jornada_jornada ON disco_mesero_jornada(jornada_id);
CREATE INDEX idx_disco_inventarios_fecha ON disco_inventarios(fecha);
CREATE INDEX idx_disco_inventarios_creado ON disco_inventarios(creado_en DESC);
CREATE INDEX idx_disco_linea_inv_inventario ON disco_linea_inventario(inventario_id);

CREATE INDEX idx_disco_mesas_estado ON disco_mesas(estado);
CREATE INDEX idx_disco_mesas_mesero ON disco_mesas(mesero_id);
CREATE INDEX idx_disco_pedidos_mesa ON disco_pedidos(mesa_id);
CREATE INDEX idx_disco_pedidos_mesero ON disco_pedidos(mesero_id);
CREATE INDEX idx_disco_pedidos_estado ON disco_pedidos(estado);
CREATE INDEX idx_disco_pedidos_fecha ON disco_pedidos(jornada_fecha);
CREATE INDEX idx_disco_pedidos_creado ON disco_pedidos(creado_en DESC);
CREATE INDEX idx_disco_linea_pedido_pedido ON disco_linea_pedido(pedido_id);
CREATE INDEX idx_disco_cuenta_mesa_mesa ON disco_cuenta_mesa(mesa_id);
CREATE INDEX idx_disco_cuenta_mesa_estado ON disco_cuenta_mesa(estado);

-- ─── 7. DATOS INICIALES: PRODUCTOS ───

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

-- ─── 8. DATOS INICIALES: MESEROS ───

INSERT INTO disco_meseros (nombre, color, avatar) VALUES
    ('Gabriel', '#FF6B35', 'GA'),
    ('Carlos',  '#4ECDC4', 'CA'),
    ('Loraine', '#FFE66D', 'LO'),
    ('Luis',    '#A8E6CF', 'LU'),
    ('Barra',   '#C3B1E1', 'BA');

-- ─── 9. DATOS INICIALES: MESAS (10 mesas por defecto) ───

INSERT INTO disco_mesas (numero, nombre) VALUES
    (1, 'Mesa 1'),
    (2, 'Mesa 2'),
    (3, 'Mesa 3'),
    (4, 'Mesa 4'),
    (5, 'Mesa 5'),
    (6, 'Mesa 6'),
    (7, 'Mesa 7'),
    (8, 'Mesa 8'),
    (9, 'Mesa 9'),
    (10, 'Mesa 10');

-- ─── 10. USUARIOS MESEROS (auth_users) ───
-- Password: monastery123 (bcrypt hash)
-- Cada mesero tiene su usuario para login desde la tablet

INSERT INTO auth_users (email, username, password, name, role, is_active) VALUES
    ('gabriel@monastery.co', 'gabriel', '$2a$10$N9qo8uLOickgx2ZMRZoMye3ssIGhGBGQkLnIJi.GUvC1gE5Qm3K2S', 'Gabriel', 'MESERO', true),
    ('carlos@monastery.co',  'carlos',  '$2a$10$N9qo8uLOickgx2ZMRZoMye3ssIGhGBGQkLnIJi.GUvC1gE5Qm3K2S', 'Carlos',  'MESERO', true),
    ('loraine@monastery.co', 'loraine', '$2a$10$N9qo8uLOickgx2ZMRZoMye3ssIGhGBGQkLnIJi.GUvC1gE5Qm3K2S', 'Loraine', 'MESERO', true),
    ('luis@monastery.co',    'luis',    '$2a$10$N9qo8uLOickgx2ZMRZoMye3ssIGhGBGQkLnIJi.GUvC1gE5Qm3K2S', 'Luis',    'MESERO', true)
ON CONFLICT (email) DO NOTHING;
