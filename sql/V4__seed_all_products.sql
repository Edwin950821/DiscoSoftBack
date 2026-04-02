-- =============================================
-- Monastery Club - Seed completo de productos
-- Basado en la Tabla de Precios del Excel Marzo 2026
-- Ejecutar en Supabase SQL Editor o DBeaver
-- BD: monastery
-- =============================================

DELETE FROM productos;

INSERT INTO productos (nombre, precio, activo, created_at, updated_at) VALUES
    ('Antioqueño Litro Tapa Verde', 150000, true, NOW(), NOW()),
    ('Antioqueño 750 Ml Verde', 120000, true, NOW(), NOW()),
    ('Antioqueño 750 Ml Azul', 120000, true, NOW(), NOW()),
    ('Antioqueño 750 Ml Amarillo', 120000, true, NOW(), NOW()),
    ('Antioqueño Litro Amarillo', 150000, true, NOW(), NOW()),
    ('Media Aguardiente', 70000, true, NOW(), NOW()),
    ('Medellin Pipona', 130000, true, NOW(), NOW()),
    ('Medellin Litro', 160000, true, NOW(), NOW()),
    ('Medellin Media', 70000, true, NOW(), NOW()),
    ('Medellin 5 Años', 160000, true, NOW(), NOW()),
    ('Medellin 8 Años', 180000, true, NOW(), NOW()),
    ('Old Parr 1 Litro', 320000, true, NOW(), NOW()),
    ('Old Parr 750 Ml', 280000, true, NOW(), NOW()),
    ('Sello Rojo 1lt', 200000, true, NOW(), NOW()),
    ('Sello Rojo 700 Ml', 170000, true, NOW(), NOW()),
    ('Sello Negro 700 Ml', 260000, true, NOW(), NOW()),
    ('Buchanan''s Deluxe 750 Ml', 280000, true, NOW(), NOW()),
    ('Buchanan''s Deluxe 375 Ml', 160000, true, NOW(), NOW()),
    ('Buchanan''s Master 750 Ml', 320000, true, NOW(), NOW()),
    ('Buchanan''s 18 Años', 500000, true, NOW(), NOW()),
    ('Black & White 700 Ml', 110000, true, NOW(), NOW()),
    ('Tequila Jose Cuervo 700 Ml', 230000, true, NOW(), NOW()),
    ('Don Julio Reposado', 420000, true, NOW(), NOW()),
    ('Don Julio 70', 520000, true, NOW(), NOW()),
    ('Smirnoff Lulo', 140000, true, NOW(), NOW()),
    ('Aguila Negra 330ml', 5000, true, NOW(), NOW()),
    ('Aguila Light', 5000, true, NOW(), NOW()),
    ('Costeñita 330ml', 5000, true, NOW(), NOW()),
    ('Coronita 355ml', 6000, true, NOW(), NOW()),
    ('Club Colombia', 6000, true, NOW(), NOW()),
    ('Heineken', 6000, true, NOW(), NOW()),
    ('Budweiser', 6000, true, NOW(), NOW()),
    ('Stella Artois', 10000, true, NOW(), NOW()),
    ('Smirnoff Ice', 14000, true, NOW(), NOW()),
    ('Agua', 5000, true, NOW(), NOW()),
    ('Coca Cola', 5000, true, NOW(), NOW()),
    ('Soda', 5000, true, NOW(), NOW()),
    ('Gatorade', 7000, true, NOW(), NOW()),
    ('Electrolit', 12000, true, NOW(), NOW()),
    ('Redbull', 15000, true, NOW(), NOW()),
    ('Bonfiest', 6000, true, NOW(), NOW()),
    ('Vaso Michelado', 3000, true, NOW(), NOW()),
    ('Bombon', 1000, true, NOW(), NOW()),
    ('Detodito', 7000, true, NOW(), NOW()),
    ('Mani', 3000, true, NOW(), NOW()),
    ('Chicle', 2000, true, NOW(), NOW()),
    ('Vaper', 60000, true, NOW(), NOW());
