-- Nombre:                 02_datos_iniciales.sql
-- Descripción:            Datos mínimos para que el sistema funcione: consecutivos de documentos y categorías de ejemplo.
--                         El usuario administrador inicial lo crea crear_bd.sh con una clave tomada de una variable de entorno.
-- Fecha de creación:      2026-10-14
-- Fecha de actualización: 2026-10-14
-- Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
-- Versión:                0.1.0

BEGIN;

INSERT INTO consecutivo (tipo_documento, prefijo, siguiente) VALUES
    ('COMPRA',     'COM', 1),
    ('VENTA',      'VEN', 1),
    ('DEVOLUCION', 'DEV', 1),
    ('AJUSTE',     'AJU', 1)
ON CONFLICT (tipo_documento) DO NOTHING;

INSERT INTO categoria (nombre) VALUES
    ('Herramientas manuales'),
    ('Herramientas eléctricas'),
    ('Eléctricos'),
    ('Plomería'),
    ('Pinturas'),
    ('Tornillería'),
    ('Construcción')
ON CONFLICT DO NOTHING;

COMMIT;
