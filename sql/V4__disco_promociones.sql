CREATE TABLE disco_promociones (
    id                    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    nombre                VARCHAR(100)  NOT NULL,
    compra_producto_ids   TEXT          NOT NULL,
    compra_cantidad       INTEGER       NOT NULL,
    regalo_producto_id    UUID          NOT NULL REFERENCES disco_productos(id),
    regalo_cantidad       INTEGER       NOT NULL DEFAULT 1,
    activa                BOOLEAN       NOT NULL DEFAULT false,
    creado_en             TIMESTAMP     NOT NULL DEFAULT NOW()
);

ALTER TABLE disco_cuenta_mesa ADD COLUMN descuento_promo INTEGER NOT NULL DEFAULT 0;
