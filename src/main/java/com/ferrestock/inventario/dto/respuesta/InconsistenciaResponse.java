/*
 * Nombre:                 InconsistenciaResponse.java
 * Descripción:            DTO de salida de la verificación de consistencia entre productos y kardex.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.dto.respuesta;

import java.math.BigDecimal;

/**
 * @param productoId          producto
 * @param codigo              código
 * @param existencias         existencias guardadas
 * @param existenciasKardex   entradas menos salidas del kardex
 * @param costoPromedio       CPP guardado
 * @param costoPromedioKardex CPP del último movimiento
 */
public record InconsistenciaResponse(Long productoId, String codigo, BigDecimal existencias, BigDecimal existenciasKardex,
                                     BigDecimal costoPromedio, BigDecimal costoPromedioKardex) {
}
