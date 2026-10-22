-- Nombre:                 90_datos_demo.sql
-- Descripción:            Datos de demostración coherentes con las reglas del sistema: 15 productos, 7 terceros, inventario
--                         inicial, 4 compras, 10 ventas, 2 devoluciones y un ajuste de salida, con su kardex (CPP calculado
--                         en orden cronológico), consecutivos y auditoría. Requiere una base recién creada con 01 y 02 y el
--                         usuario admin (id 1). Se cargó en producción (Neon) el 2026-10-22; la verificación dio 0 diferencias.
-- Fecha de creación:      2026-10-22
-- Fecha de actualización: 2026-10-22
-- Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
-- Versión:                1.0.0

BEGIN;
INSERT INTO tercero (id, tipo_persona, tipo_documento, numero_documento, digito_verificacion, nombre, telefono, correo, direccion, es_cliente, es_proveedor, creado_en, actualizado_en, creado_por_id, actualizado_por_id) OVERRIDING SYSTEM VALUE VALUES (1, 'JURIDICA', 'NIT', '900512347', '2', 'Distribuidora Ferretera del Valle SAS', '602 555 1020', 'ventas@ferrevalle.co', 'Cl 15 # 32-10, Cali', FALSE, TRUE, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(2, 'JURIDICA', 'NIT', '860001022', '7', 'Pinturas Andinas SA', '601 744 2200', 'pedidos@pinturasandinas.co', 'Av 68 # 13-45, Bogotá', FALSE, TRUE, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(3, 'JURIDICA', 'NIT', '890903938', '8', 'Eléctricos y Cables de Colombia SAS', '604 448 9000', 'comercial@electricables.co', 'Cra 50 # 10-20, Medellín', TRUE, TRUE, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(4, 'NATURAL', 'CC', '1020304050', NULL, 'Carlos Andrés Pérez Gómez', '3104567890', 'carlos.perez@correo.co', 'Cra 7 # 45-12, Bogotá', TRUE, FALSE, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(5, 'NATURAL', 'CC', '52789456', NULL, 'Luz Marina Rodríguez', '3157894561', NULL, 'Cl 80 # 20-15, Bogotá', TRUE, FALSE, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(6, 'JURIDICA', 'NIT', '901234567', '7', 'Constructora Horizonte SAS', '601 300 4500', 'compras@horizonte.co', 'Cl 100 # 15-30, Bogotá', TRUE, FALSE, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(7, 'NATURAL', 'CE', '4567890', NULL, 'Jorge Luis Medina', '3201234567', NULL, NULL, TRUE, FALSE, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1);

INSERT INTO producto (id, codigo, nombre, categoria_id, unidad_medida, permite_fraccion, precio_venta, creado_en, actualizado_en, creado_por_id, actualizado_por_id) OVERRIDING SYSTEM VALUE VALUES (1, 'HM-MAR16', 'Martillo de uña 16 oz mango fibra', 1, 'UNIDAD', FALSE, 38500.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(2, 'HM-DES6', 'Juego de destornilladores x6', 1, 'UNIDAD', FALSE, 42900.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(3, 'HM-ALI10', 'Alicate universal 8 pulgadas', 1, 'UNIDAD', FALSE, 27500.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(4, 'HE-TAL12', 'Taladro percutor 1/2 pulgada 650 W', 2, 'UNIDAD', FALSE, 289000.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(5, 'HE-PUL45', 'Pulidora angular 4 1/2 pulgadas 850 W', 2, 'UNIDAD', FALSE, 235000.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(6, 'EL-CAB12', 'Cable eléctrico THHN calibre 12', 3, 'METRO', TRUE, 3200.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(7, 'EL-INT01', 'Interruptor sencillo blanco', 3, 'UNIDAD', FALSE, 8900.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(8, 'PL-TUB12', 'Tubo PVC presión 1/2 pulgada x 6 m', 4, 'UNIDAD', FALSE, 18900.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(9, 'PL-LLA12', 'Llave de paso 1/2 pulgada', 4, 'UNIDAD', FALSE, 24500.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(10, 'PI-VIN1G', 'Pintura vinilo blanco galón', 5, 'GALON', FALSE, 64900.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(11, 'PI-BRO3', 'Brocha de 3 pulgadas', 5, 'UNIDAD', FALSE, 9800.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(12, 'TO-DRY6', 'Tornillo drywall 6x1 caja x100', 6, 'CAJA', FALSE, 12500.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(13, 'TO-PUN1', 'Puntilla de acero 1 pulgada', 6, 'KILOGRAMO', TRUE, 14800.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(14, 'CO-CEM50', 'Cemento gris 50 kg', 7, 'UNIDAD', FALSE, 33900.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1),
(15, 'CO-ARE', 'Arena de río (bulto)', 7, 'UNIDAD', FALSE, 9500.00, '2026-10-10 13:00:00+00', '2026-10-10 13:00:00+00', 1, 1);

INSERT INTO ajuste (id, consecutivo, producto_id, tipo, cantidad, costo_unitario, motivo, fecha, creado_por_id, creado_en) OVERRIDING SYSTEM VALUE VALUES (1, 'AJU-000001', 1, 'ENTRADA', 25.000, 21000.0000, 'Inventario inicial', '2026-10-11 13:05:00+00', 1, '2026-10-11 13:05:00+00'),
(2, 'AJU-000002', 2, 'ENTRADA', 15.000, 24500.0000, 'Inventario inicial', '2026-10-11 13:10:00+00', 1, '2026-10-11 13:10:00+00'),
(3, 'AJU-000003', 3, 'ENTRADA', 30.000, 15200.0000, 'Inventario inicial', '2026-10-11 13:15:00+00', 1, '2026-10-11 13:15:00+00'),
(4, 'AJU-000004', 4, 'ENTRADA', 6.000, 175000.0000, 'Inventario inicial', '2026-10-11 13:20:00+00', 1, '2026-10-11 13:20:00+00'),
(5, 'AJU-000005', 5, 'ENTRADA', 5.000, 142000.0000, 'Inventario inicial', '2026-10-11 13:25:00+00', 1, '2026-10-11 13:25:00+00'),
(6, 'AJU-000006', 6, 'ENTRADA', 250.000, 1650.0000, 'Inventario inicial', '2026-10-11 13:30:00+00', 1, '2026-10-11 13:30:00+00'),
(7, 'AJU-000007', 7, 'ENTRADA', 80.000, 4300.0000, 'Inventario inicial', '2026-10-11 13:35:00+00', 1, '2026-10-11 13:35:00+00'),
(8, 'AJU-000008', 8, 'ENTRADA', 40.000, 10200.0000, 'Inventario inicial', '2026-10-11 13:40:00+00', 1, '2026-10-11 13:40:00+00'),
(9, 'AJU-000009', 9, 'ENTRADA', 20.000, 13800.0000, 'Inventario inicial', '2026-10-11 13:45:00+00', 1, '2026-10-11 13:45:00+00'),
(10, 'AJU-000010', 10, 'ENTRADA', 18.000, 38500.0000, 'Inventario inicial', '2026-10-11 13:50:00+00', 1, '2026-10-11 13:50:00+00'),
(11, 'AJU-000011', 11, 'ENTRADA', 60.000, 4900.0000, 'Inventario inicial', '2026-10-11 13:55:00+00', 1, '2026-10-11 13:55:00+00'),
(12, 'AJU-000012', 12, 'ENTRADA', 45.000, 6300.0000, 'Inventario inicial', '2026-10-11 14:00:00+00', 1, '2026-10-11 14:00:00+00'),
(13, 'AJU-000013', 13, 'ENTRADA', 35.500, 8700.0000, 'Inventario inicial', '2026-10-11 14:05:00+00', 1, '2026-10-11 14:05:00+00'),
(14, 'AJU-000014', 14, 'ENTRADA', 60.000, 26500.0000, 'Inventario inicial', '2026-10-11 14:10:00+00', 1, '2026-10-11 14:10:00+00'),
(15, 'AJU-000015', 15, 'ENTRADA', 100.000, 5200.0000, 'Inventario inicial', '2026-10-11 14:15:00+00', 1, '2026-10-11 14:15:00+00'),
(16, 'AJU-000016', 11, 'SALIDA', 2.000, 4982.4742, 'Unidad dañada en bodega', '2026-10-16 21:00:00+00', 1, '2026-10-16 21:00:00+00');

INSERT INTO compra (id, consecutivo, proveedor_id, fecha, observacion, total, estado, creado_por_id, creado_en) OVERRIDING SYSTEM VALUE VALUES (1, 'COM-000001', 1, '2026-10-13 15:00:00+00', 'Factura FV-10231', 1005000.00, 'REGISTRADA', 1, '2026-10-13 15:00:00+00'),
(2, 'COM-000002', 3, '2026-10-14 15:00:00+00', 'Factura EC-5521', 1504000.00, 'REGISTRADA', 1, '2026-10-14 15:00:00+00'),
(3, 'COM-000003', 2, '2026-10-15 15:00:00+00', 'Factura PA-889', 1161600.00, 'REGISTRADA', 1, '2026-10-15 15:00:00+00'),
(4, 'COM-000004', 1, '2026-10-16 15:00:00+00', 'Factura FV-10307', 1637000.00, 'REGISTRADA', 1, '2026-10-16 15:00:00+00');

INSERT INTO compra_linea (compra_id, producto_id, cantidad, costo_unitario, subtotal) VALUES (1, 1, 20.000, 22500.0000, 450000.00),
(1, 2, 10.000, 25000.0000, 250000.00),
(1, 12, 50.000, 6100.0000, 305000.00),
(2, 6, 300.000, 1720.0000, 516000.00),
(2, 7, 60.000, 4400.0000, 264000.00),
(2, 4, 4.000, 181000.0000, 724000.00),
(3, 10, 24.000, 39900.0000, 957600.00),
(3, 11, 40.000, 5100.0000, 204000.00),
(4, 14, 40.000, 27200.0000, 1088000.00),
(4, 8, 30.000, 10500.0000, 315000.00),
(4, 3, 15.000, 15600.0000, 234000.00);

INSERT INTO venta (id, consecutivo, cliente_id, fecha, total, medio_pago, valor_recibido, cambio, estado, creado_por_id, creado_en) OVERRIDING SYSTEM VALUE VALUES (1, 'VEN-000001', 4, '2026-10-13 17:00:00+00', 114500.00, 'EFECTIVO', 120000.00, 5500.00, 'REGISTRADA', 1, '2026-10-13 17:00:00+00'),
(2, 'VEN-000002', 5, '2026-10-13 19:00:00+00', 159200.00, 'EFECTIVO', 160000.00, 800.00, 'REGISTRADA', 1, '2026-10-13 19:00:00+00'),
(3, 'VEN-000003', 6, '2026-10-14 16:00:00+00', 1152000.00, 'EFECTIVO', 1160000.00, 8000.00, 'REGISTRADA', 1, '2026-10-14 16:00:00+00'),
(4, 'VEN-000004', 4, '2026-10-14 18:00:00+00', 135000.00, 'EFECTIVO', 140000.00, 5000.00, 'REGISTRADA', 1, '2026-10-14 18:00:00+00'),
(5, 'VEN-000005', 7, '2026-10-15 15:00:00+00', 70400.00, 'EFECTIVO', 71000.00, 600.00, 'REGISTRADA', 1, '2026-10-15 15:00:00+00'),
(6, 'VEN-000006', 3, '2026-10-15 17:00:00+00', 524000.00, 'EFECTIVO', 524000.00, 0.00, 'REGISTRADA', 1, '2026-10-15 17:00:00+00'),
(7, 'VEN-000007', 5, '2026-10-15 19:00:00+00', 58300.00, 'EFECTIVO', 60000.00, 1700.00, 'REGISTRADA', 1, '2026-10-15 19:00:00+00'),
(8, 'VEN-000008', 6, '2026-10-16 16:00:00+00', 801200.00, 'EFECTIVO', 810000.00, 8800.00, 'REGISTRADA', 1, '2026-10-16 16:00:00+00'),
(9, 'VEN-000009', 4, '2026-10-16 18:00:00+00', 58100.00, 'EFECTIVO', 60000.00, 1900.00, 'REGISTRADA', 1, '2026-10-16 18:00:00+00'),
(10, 'VEN-000010', 7, '2026-10-16 19:00:00+00', 129000.00, 'EFECTIVO', 150000.00, 21000.00, 'REGISTRADA', 1, '2026-10-16 19:00:00+00');

INSERT INTO venta_linea (id, venta_id, producto_id, cantidad, precio_unitario, costo_unitario, subtotal, cantidad_devuelta) OVERRIDING SYSTEM VALUE VALUES (1, 1, 1, 2.000, 38500.00, 21666.6667, 77000.00, 0.000),
(2, 1, 12, 3.000, 12500.00, 6194.7368, 37500.00, 0.000),
(3, 2, 10, 2.000, 64900.00, 38500.0000, 129800.00, 0.000),
(4, 2, 11, 3.000, 9800.00, 4900.0000, 29400.00, 0.000),
(5, 3, 14, 20.000, 33900.00, 26500.0000, 678000.00, 0.000),
(6, 3, 15, 30.000, 9500.00, 5200.0000, 285000.00, 0.000),
(7, 3, 8, 10.000, 18900.00, 10200.0000, 189000.00, 0.000),
(8, 4, 6, 25.500, 3200.00, 1688.1818, 81600.00, 0.000),
(9, 4, 7, 6.000, 8900.00, 4342.8571, 53400.00, 0.000),
(10, 5, 3, 1.000, 27500.00, 15200.0000, 27500.00, 0.000),
(11, 5, 2, 1.000, 42900.00, 24700.0000, 42900.00, 0.000),
(12, 6, 4, 1.000, 289000.00, 177400.0000, 289000.00, 0.000),
(13, 6, 5, 1.000, 235000.00, 142000.0000, 235000.00, 0.000),
(14, 7, 13, 2.250, 14800.00, 8700.0000, 33300.00, 0.000),
(15, 7, 12, 2.000, 12500.00, 6194.7368, 25000.00, 0.000),
(16, 8, 14, 15.000, 33900.00, 26850.0000, 508500.00, 0.000),
(17, 8, 9, 4.000, 24500.00, 13800.0000, 98000.00, 0.000),
(18, 8, 10, 3.000, 64900.00, 39340.0000, 194700.00, 0.000),
(19, 9, 1, 1.000, 38500.00, 21666.6667, 38500.00, 0.000),
(20, 9, 11, 2.000, 9800.00, 4982.4742, 19600.00, 0.000),
(21, 10, 6, 12.000, 3200.00, 1688.1818, 38400.00, 0.000),
(22, 10, 7, 4.000, 8900.00, 4342.8571, 35600.00, 0.000),
(23, 10, 3, 2.000, 27500.00, 15336.3636, 55000.00, 0.000);

INSERT INTO devolucion (id, consecutivo, cliente_id, venta_id, motivo, fecha, validada_por_id, creado_por_id, creado_en) OVERRIDING SYSTEM VALUE VALUES (1, 'DEV-000001', 4, 4, 'Cable con aislante defectuoso', '2026-10-15 14:00:00+00', 1, 1, '2026-10-15 14:00:00+00'),
(2, 'DEV-000002', 4, 9, 'Martillo con mango fisurado', '2026-10-16 20:00:00+00', 1, 1, '2026-10-16 20:00:00+00');

INSERT INTO devolucion_linea (devolucion_id, producto_id, venta_linea_id, cantidad, resultado, costo_unitario) VALUES (1, 6, 8, 5.500, 'ACEPTADA', 1688.1818),
(1, 7, 9, 1.000, 'RECHAZADA', NULL),
(2, 1, 19, 1.000, 'ACEPTADA', 21666.6667);

INSERT INTO movimiento_inventario (id, producto_id, tipo, naturaleza, cantidad, costo_unitario, saldo_resultante, costo_promedio_anterior, costo_promedio_resultante, documento_tipo, documento_id, usuario_id, fecha) OVERRIDING SYSTEM VALUE VALUES (1, 1, 'AJUSTE_ENTRADA', 'ENTRADA', 25.000, 21000.0000, 25.000, 0.0000, 21000.0000, 'AJUSTE', 1, 1, '2026-10-11 13:05:00+00'),
(2, 2, 'AJUSTE_ENTRADA', 'ENTRADA', 15.000, 24500.0000, 15.000, 0.0000, 24500.0000, 'AJUSTE', 2, 1, '2026-10-11 13:10:00+00'),
(3, 3, 'AJUSTE_ENTRADA', 'ENTRADA', 30.000, 15200.0000, 30.000, 0.0000, 15200.0000, 'AJUSTE', 3, 1, '2026-10-11 13:15:00+00'),
(4, 4, 'AJUSTE_ENTRADA', 'ENTRADA', 6.000, 175000.0000, 6.000, 0.0000, 175000.0000, 'AJUSTE', 4, 1, '2026-10-11 13:20:00+00'),
(5, 5, 'AJUSTE_ENTRADA', 'ENTRADA', 5.000, 142000.0000, 5.000, 0.0000, 142000.0000, 'AJUSTE', 5, 1, '2026-10-11 13:25:00+00'),
(6, 6, 'AJUSTE_ENTRADA', 'ENTRADA', 250.000, 1650.0000, 250.000, 0.0000, 1650.0000, 'AJUSTE', 6, 1, '2026-10-11 13:30:00+00'),
(7, 7, 'AJUSTE_ENTRADA', 'ENTRADA', 80.000, 4300.0000, 80.000, 0.0000, 4300.0000, 'AJUSTE', 7, 1, '2026-10-11 13:35:00+00'),
(8, 8, 'AJUSTE_ENTRADA', 'ENTRADA', 40.000, 10200.0000, 40.000, 0.0000, 10200.0000, 'AJUSTE', 8, 1, '2026-10-11 13:40:00+00'),
(9, 9, 'AJUSTE_ENTRADA', 'ENTRADA', 20.000, 13800.0000, 20.000, 0.0000, 13800.0000, 'AJUSTE', 9, 1, '2026-10-11 13:45:00+00'),
(10, 10, 'AJUSTE_ENTRADA', 'ENTRADA', 18.000, 38500.0000, 18.000, 0.0000, 38500.0000, 'AJUSTE', 10, 1, '2026-10-11 13:50:00+00'),
(11, 11, 'AJUSTE_ENTRADA', 'ENTRADA', 60.000, 4900.0000, 60.000, 0.0000, 4900.0000, 'AJUSTE', 11, 1, '2026-10-11 13:55:00+00'),
(12, 12, 'AJUSTE_ENTRADA', 'ENTRADA', 45.000, 6300.0000, 45.000, 0.0000, 6300.0000, 'AJUSTE', 12, 1, '2026-10-11 14:00:00+00'),
(13, 13, 'AJUSTE_ENTRADA', 'ENTRADA', 35.500, 8700.0000, 35.500, 0.0000, 8700.0000, 'AJUSTE', 13, 1, '2026-10-11 14:05:00+00'),
(14, 14, 'AJUSTE_ENTRADA', 'ENTRADA', 60.000, 26500.0000, 60.000, 0.0000, 26500.0000, 'AJUSTE', 14, 1, '2026-10-11 14:10:00+00'),
(15, 15, 'AJUSTE_ENTRADA', 'ENTRADA', 100.000, 5200.0000, 100.000, 0.0000, 5200.0000, 'AJUSTE', 15, 1, '2026-10-11 14:15:00+00'),
(16, 1, 'COMPRA', 'ENTRADA', 20.000, 22500.0000, 45.000, 21000.0000, 21666.6667, 'COMPRA', 1, 1, '2026-10-13 15:00:00+00'),
(17, 2, 'COMPRA', 'ENTRADA', 10.000, 25000.0000, 25.000, 24500.0000, 24700.0000, 'COMPRA', 1, 1, '2026-10-13 15:00:00+00'),
(18, 12, 'COMPRA', 'ENTRADA', 50.000, 6100.0000, 95.000, 6300.0000, 6194.7368, 'COMPRA', 1, 1, '2026-10-13 15:00:00+00'),
(19, 1, 'VENTA', 'SALIDA', 2.000, 21666.6667, 43.000, 21666.6667, 21666.6667, 'VENTA', 1, 1, '2026-10-13 17:00:00+00'),
(20, 12, 'VENTA', 'SALIDA', 3.000, 6194.7368, 92.000, 6194.7368, 6194.7368, 'VENTA', 1, 1, '2026-10-13 17:00:00+00'),
(21, 10, 'VENTA', 'SALIDA', 2.000, 38500.0000, 16.000, 38500.0000, 38500.0000, 'VENTA', 2, 1, '2026-10-13 19:00:00+00'),
(22, 11, 'VENTA', 'SALIDA', 3.000, 4900.0000, 57.000, 4900.0000, 4900.0000, 'VENTA', 2, 1, '2026-10-13 19:00:00+00'),
(23, 6, 'COMPRA', 'ENTRADA', 300.000, 1720.0000, 550.000, 1650.0000, 1688.1818, 'COMPRA', 2, 1, '2026-10-14 15:00:00+00'),
(24, 7, 'COMPRA', 'ENTRADA', 60.000, 4400.0000, 140.000, 4300.0000, 4342.8571, 'COMPRA', 2, 1, '2026-10-14 15:00:00+00'),
(25, 4, 'COMPRA', 'ENTRADA', 4.000, 181000.0000, 10.000, 175000.0000, 177400.0000, 'COMPRA', 2, 1, '2026-10-14 15:00:00+00'),
(26, 14, 'VENTA', 'SALIDA', 20.000, 26500.0000, 40.000, 26500.0000, 26500.0000, 'VENTA', 3, 1, '2026-10-14 16:00:00+00'),
(27, 15, 'VENTA', 'SALIDA', 30.000, 5200.0000, 70.000, 5200.0000, 5200.0000, 'VENTA', 3, 1, '2026-10-14 16:00:00+00'),
(28, 8, 'VENTA', 'SALIDA', 10.000, 10200.0000, 30.000, 10200.0000, 10200.0000, 'VENTA', 3, 1, '2026-10-14 16:00:00+00'),
(29, 6, 'VENTA', 'SALIDA', 25.500, 1688.1818, 524.500, 1688.1818, 1688.1818, 'VENTA', 4, 1, '2026-10-14 18:00:00+00'),
(30, 7, 'VENTA', 'SALIDA', 6.000, 4342.8571, 134.000, 4342.8571, 4342.8571, 'VENTA', 4, 1, '2026-10-14 18:00:00+00'),
(31, 6, 'DEVOLUCION', 'ENTRADA', 5.500, 1688.1818, 530.000, 1688.1818, 1688.1818, 'DEVOLUCION', 1, 1, '2026-10-15 14:00:00+00'),
(32, 10, 'COMPRA', 'ENTRADA', 24.000, 39900.0000, 40.000, 38500.0000, 39340.0000, 'COMPRA', 3, 1, '2026-10-15 15:00:00+00'),
(33, 11, 'COMPRA', 'ENTRADA', 40.000, 5100.0000, 97.000, 4900.0000, 4982.4742, 'COMPRA', 3, 1, '2026-10-15 15:00:00+00'),
(34, 3, 'VENTA', 'SALIDA', 1.000, 15200.0000, 29.000, 15200.0000, 15200.0000, 'VENTA', 5, 1, '2026-10-15 15:00:00+00'),
(35, 2, 'VENTA', 'SALIDA', 1.000, 24700.0000, 24.000, 24700.0000, 24700.0000, 'VENTA', 5, 1, '2026-10-15 15:00:00+00'),
(36, 4, 'VENTA', 'SALIDA', 1.000, 177400.0000, 9.000, 177400.0000, 177400.0000, 'VENTA', 6, 1, '2026-10-15 17:00:00+00'),
(37, 5, 'VENTA', 'SALIDA', 1.000, 142000.0000, 4.000, 142000.0000, 142000.0000, 'VENTA', 6, 1, '2026-10-15 17:00:00+00'),
(38, 13, 'VENTA', 'SALIDA', 2.250, 8700.0000, 33.250, 8700.0000, 8700.0000, 'VENTA', 7, 1, '2026-10-15 19:00:00+00'),
(39, 12, 'VENTA', 'SALIDA', 2.000, 6194.7368, 90.000, 6194.7368, 6194.7368, 'VENTA', 7, 1, '2026-10-15 19:00:00+00'),
(40, 14, 'COMPRA', 'ENTRADA', 40.000, 27200.0000, 80.000, 26500.0000, 26850.0000, 'COMPRA', 4, 1, '2026-10-16 15:00:00+00'),
(41, 8, 'COMPRA', 'ENTRADA', 30.000, 10500.0000, 60.000, 10200.0000, 10350.0000, 'COMPRA', 4, 1, '2026-10-16 15:00:00+00'),
(42, 3, 'COMPRA', 'ENTRADA', 15.000, 15600.0000, 44.000, 15200.0000, 15336.3636, 'COMPRA', 4, 1, '2026-10-16 15:00:00+00'),
(43, 14, 'VENTA', 'SALIDA', 15.000, 26850.0000, 65.000, 26850.0000, 26850.0000, 'VENTA', 8, 1, '2026-10-16 16:00:00+00'),
(44, 9, 'VENTA', 'SALIDA', 4.000, 13800.0000, 16.000, 13800.0000, 13800.0000, 'VENTA', 8, 1, '2026-10-16 16:00:00+00'),
(45, 10, 'VENTA', 'SALIDA', 3.000, 39340.0000, 37.000, 39340.0000, 39340.0000, 'VENTA', 8, 1, '2026-10-16 16:00:00+00'),
(46, 1, 'VENTA', 'SALIDA', 1.000, 21666.6667, 42.000, 21666.6667, 21666.6667, 'VENTA', 9, 1, '2026-10-16 18:00:00+00'),
(47, 11, 'VENTA', 'SALIDA', 2.000, 4982.4742, 95.000, 4982.4742, 4982.4742, 'VENTA', 9, 1, '2026-10-16 18:00:00+00'),
(48, 6, 'VENTA', 'SALIDA', 12.000, 1688.1818, 518.000, 1688.1818, 1688.1818, 'VENTA', 10, 1, '2026-10-16 19:00:00+00'),
(49, 7, 'VENTA', 'SALIDA', 4.000, 4342.8571, 130.000, 4342.8571, 4342.8571, 'VENTA', 10, 1, '2026-10-16 19:00:00+00'),
(50, 3, 'VENTA', 'SALIDA', 2.000, 15336.3636, 42.000, 15336.3636, 15336.3636, 'VENTA', 10, 1, '2026-10-16 19:00:00+00'),
(51, 1, 'DEVOLUCION', 'ENTRADA', 1.000, 21666.6667, 43.000, 21666.6667, 21666.6667, 'DEVOLUCION', 2, 1, '2026-10-16 20:00:00+00'),
(52, 11, 'AJUSTE_SALIDA', 'SALIDA', 2.000, 4982.4742, 93.000, 4982.4742, 4982.4742, 'AJUSTE', 16, 1, '2026-10-16 21:00:00+00');

INSERT INTO auditoria (usuario_id, nombre_usuario, fecha, accion, descripcion) VALUES (1, 'admin', '2026-10-11 13:05:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000001: ENTRADA de 25 en HM-MAR16. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:10:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000002: ENTRADA de 15 en HM-DES6. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:15:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000003: ENTRADA de 30 en HM-ALI10. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:20:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000004: ENTRADA de 6 en HE-TAL12. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:25:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000005: ENTRADA de 5 en HE-PUL45. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:30:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000006: ENTRADA de 250 en EL-CAB12. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:35:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000007: ENTRADA de 80 en EL-INT01. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:40:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000008: ENTRADA de 40 en PL-TUB12. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:45:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000009: ENTRADA de 20 en PL-LLA12. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:50:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000010: ENTRADA de 18 en PI-VIN1G. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 13:55:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000011: ENTRADA de 60 en PI-BRO3. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 14:00:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000012: ENTRADA de 45 en TO-DRY6. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 14:05:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000013: ENTRADA de 35.5 en TO-PUN1. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 14:10:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000014: ENTRADA de 60 en CO-CEM50. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-11 14:15:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000015: ENTRADA de 100 en CO-ARE. Motivo: Inventario inicial'),
(1, 'admin', '2026-10-13 15:00:00+00', 'COMPRA_REGISTRADA', 'Compra COM-000001 al proveedor Distribuidora Ferretera del Valle SAS: 3 línea(s) por 1005000.00'),
(1, 'admin', '2026-10-13 17:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000001 a Carlos Andrés Pérez Gómez por 114500.00 (recibido 120000.00, cambio 5500.00)'),
(1, 'admin', '2026-10-13 19:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000002 a Luz Marina Rodríguez por 159200.00 (recibido 160000.00, cambio 800.00)'),
(1, 'admin', '2026-10-14 15:00:00+00', 'COMPRA_REGISTRADA', 'Compra COM-000002 al proveedor Eléctricos y Cables de Colombia SAS: 3 línea(s) por 1504000.00'),
(1, 'admin', '2026-10-14 16:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000003 a Constructora Horizonte SAS por 1152000.00 (recibido 1160000.00, cambio 8000.00)'),
(1, 'admin', '2026-10-14 18:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000004 a Carlos Andrés Pérez Gómez por 135000.00 (recibido 140000.00, cambio 5000.00)'),
(1, 'admin', '2026-10-15 14:00:00+00', 'DEVOLUCION_REGISTRADA', 'Devolución DEV-000001 de Carlos Andrés Pérez Gómez sobre la venta VEN-000004: 1 línea(s) aceptada(s) y 1 rechazada(s), validadas por admin. Motivo: Cable con aislante defectuoso'),
(1, 'admin', '2026-10-15 15:00:00+00', 'COMPRA_REGISTRADA', 'Compra COM-000003 al proveedor Pinturas Andinas SA: 2 línea(s) por 1161600.00'),
(1, 'admin', '2026-10-15 15:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000005 a Jorge Luis Medina por 70400.00 (recibido 71000.00, cambio 600.00)'),
(1, 'admin', '2026-10-15 17:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000006 a Eléctricos y Cables de Colombia SAS por 524000.00 (recibido 524000.00, cambio 0.00)'),
(1, 'admin', '2026-10-15 19:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000007 a Luz Marina Rodríguez por 58300.00 (recibido 60000.00, cambio 1700.00)'),
(1, 'admin', '2026-10-16 15:00:00+00', 'COMPRA_REGISTRADA', 'Compra COM-000004 al proveedor Distribuidora Ferretera del Valle SAS: 3 línea(s) por 1637000.00'),
(1, 'admin', '2026-10-16 16:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000008 a Constructora Horizonte SAS por 801200.00 (recibido 810000.00, cambio 8800.00)'),
(1, 'admin', '2026-10-16 18:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000009 a Carlos Andrés Pérez Gómez por 58100.00 (recibido 60000.00, cambio 1900.00)'),
(1, 'admin', '2026-10-16 19:00:00+00', 'VENTA_REGISTRADA', 'Venta VEN-000010 a Jorge Luis Medina por 129000.00 (recibido 150000.00, cambio 21000.00)'),
(1, 'admin', '2026-10-16 20:00:00+00', 'DEVOLUCION_REGISTRADA', 'Devolución DEV-000002 de Carlos Andrés Pérez Gómez sobre la venta VEN-000009: 1 línea(s) aceptada(s) y 0 rechazada(s), validadas por admin. Motivo: Martillo con mango fisurado'),
(1, 'admin', '2026-10-16 21:00:00+00', 'AJUSTE_REGISTRADO', 'Ajuste AJU-000016: SALIDA de 2 en PI-BRO3. Motivo: Unidad dañada en bodega');

UPDATE venta_linea SET cantidad_devuelta = cantidad_devuelta + 5.500 WHERE id = 8;

UPDATE venta_linea SET cantidad_devuelta = cantidad_devuelta + 1.000 WHERE id = 19;

UPDATE producto SET existencias = 43.000, costo_promedio = 21666.6667 WHERE id = 1;

UPDATE producto SET existencias = 24.000, costo_promedio = 24700.0000 WHERE id = 2;

UPDATE producto SET existencias = 42.000, costo_promedio = 15336.3636 WHERE id = 3;

UPDATE producto SET existencias = 9.000, costo_promedio = 177400.0000 WHERE id = 4;

UPDATE producto SET existencias = 4.000, costo_promedio = 142000.0000 WHERE id = 5;

UPDATE producto SET existencias = 518.000, costo_promedio = 1688.1818 WHERE id = 6;

UPDATE producto SET existencias = 130.000, costo_promedio = 4342.8571 WHERE id = 7;

UPDATE producto SET existencias = 60.000, costo_promedio = 10350.0000 WHERE id = 8;

UPDATE producto SET existencias = 16.000, costo_promedio = 13800.0000 WHERE id = 9;

UPDATE producto SET existencias = 37.000, costo_promedio = 39340.0000 WHERE id = 10;

UPDATE producto SET existencias = 93.000, costo_promedio = 4982.4742 WHERE id = 11;

UPDATE producto SET existencias = 90.000, costo_promedio = 6194.7368 WHERE id = 12;

UPDATE producto SET existencias = 33.250, costo_promedio = 8700.0000 WHERE id = 13;

UPDATE producto SET existencias = 65.000, costo_promedio = 26850.0000 WHERE id = 14;

UPDATE producto SET existencias = 70.000, costo_promedio = 5200.0000 WHERE id = 15;

UPDATE consecutivo SET siguiente = CASE tipo_documento WHEN 'AJUSTE' THEN 17 WHEN 'COMPRA' THEN 5 WHEN 'VENTA' THEN 11 WHEN 'DEVOLUCION' THEN 3 END;

SELECT setval(pg_get_serial_sequence('tercero','id'), (SELECT max(id) FROM tercero));

SELECT setval(pg_get_serial_sequence('producto','id'), (SELECT max(id) FROM producto));

SELECT setval(pg_get_serial_sequence('compra','id'), (SELECT max(id) FROM compra));

SELECT setval(pg_get_serial_sequence('compra_linea','id'), (SELECT max(id) FROM compra_linea));

SELECT setval(pg_get_serial_sequence('venta','id'), (SELECT max(id) FROM venta));

SELECT setval(pg_get_serial_sequence('venta_linea','id'), (SELECT max(id) FROM venta_linea));

SELECT setval(pg_get_serial_sequence('devolucion','id'), (SELECT max(id) FROM devolucion));

SELECT setval(pg_get_serial_sequence('devolucion_linea','id'), (SELECT max(id) FROM devolucion_linea));

SELECT setval(pg_get_serial_sequence('ajuste','id'), (SELECT max(id) FROM ajuste));

SELECT setval(pg_get_serial_sequence('movimiento_inventario','id'), (SELECT max(id) FROM movimiento_inventario));

COMMIT;
