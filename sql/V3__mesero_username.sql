-- V3: Agregar campo username a disco_meseros
ALTER TABLE disco_meseros ADD COLUMN username VARCHAR(50) UNIQUE;
