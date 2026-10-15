/*
 * Nombre:                 MovimientoResponse.java
 * Descripción:            DTO de salida de un movimiento del kardex; los costos solo se llenan para el ADMIN (DP-15).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param id                      movimiento
 * @param fecha                   fecha y hora (UTC)
 * @param tipo                    tipo de movimiento
 * @param naturaleza              ENTRADA o SALIDA
 * @param cantidad                cantidad
 * @param saldoResultante         existencias después del movimiento
 * @param documentoTipo           documento origen
 * @param documentoId             id del documento origen
 * @param usuario                 nombre de usuario responsable
 * @param costoUnitario           costo con que entró o salió (solo ADMIN)
 * @param costoPromedioResultante CPP después del movimiento (solo ADMIN)
 */
public record MovimientoResponse(Long id, Instant fecha, String tipo, String naturaleza, BigDecimal cantidad,
                                 BigDecimal saldoResultante, String documentoTipo, Long documentoId, String usuario,
                                 BigDecimal costoUnitario, BigDecimal costoPromedioResultante) {
}
