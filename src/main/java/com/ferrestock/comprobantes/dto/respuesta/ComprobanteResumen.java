/*
 * Nombre:                 ComprobanteResumen.java
 * Descripción:            DTO de salida de la búsqueda unificada de comprobantes (RF-DOC-005).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.10.0
 */
package com.ferrestock.comprobantes.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param tipo        COMPRA, VENTA, DEVOLUCION o AJUSTE
 * @param consecutivo número del documento
 * @param fecha       fecha y hora (UTC)
 * @param tercero     cliente, proveedor o, en un ajuste, el producto
 * @param total       total (nulo en devoluciones y ajustes)
 * @param estado      REGISTRADA o ANULADA
 */
public record ComprobanteResumen(String tipo, String consecutivo, Instant fecha, String tercero, BigDecimal total,
                                 String estado) {
}
