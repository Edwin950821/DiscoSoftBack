-- ==========================================
-- V7: Tipo de negocio (DISCOTECA / BILLAR)
-- ==========================================
-- Agrega la columna `tipo` a la tabla `negocios` para distinguir
-- entre discotecas y billares. Por defecto todos los negocios
-- existentes quedan como DISCOTECA — actualizar manualmente
-- los billares con el UPDATE al final del archivo.
--
-- Idempotente: se puede correr varias veces sin romper nada.
-- ==========================================

-- 1. Agregar columna con default DISCOTECA (no rompe registros existentes)
ALTER TABLE negocios ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'DISCOTECA';

-- 2. Constraint para que solo acepte los valores válidos
ALTER TABLE negocios DROP CONSTRAINT IF EXISTS chk_negocios_tipo;
ALTER TABLE negocios ADD CONSTRAINT chk_negocios_tipo CHECK (tipo IN ('DISCOTECA', 'BILLAR'));

-- 3. (Opcional) Marcar el billar — descomentar y ajustar slug
-- UPDATE negocios SET tipo = 'BILLAR' WHERE slug = 'billar-baranoa';
