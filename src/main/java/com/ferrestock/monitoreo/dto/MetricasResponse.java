/*
 * Nombre:                 MetricasResponse.java
 * Descripción:            DTO de salida de las métricas de operación del sistema (RF-MON-005, DP-18).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.13.0
 */
package com.ferrestock.monitoreo.dto;

import java.math.BigDecimal;

/**
 * Métricas del día (zona de Bogotá) y de las últimas 24 horas.
 *
 * @param ventasHoy              ventas registradas hoy
 * @param totalVentasHoy         valor vendido hoy (sin anuladas)
 * @param comprasHoy             compras registradas hoy
 * @param devolucionesHoy        devoluciones registradas hoy
 * @param anulacionesHoy         compras y ventas anuladas hoy
 * @param productosSinExistencias productos activos sin existencias
 * @param valorInventario        valor del inventario al costo promedio
 * @param errores24h             errores no controlados en las últimas 24 horas
 * @param erroresReportadosTaiga errores de las últimas 24 horas reportados a Taiga
 * @param accesosDenegados24h    intentos de acceso sin permiso en las últimas 24 horas
 * @param loginsFallidos24h      inicios de sesión fallidos en las últimas 24 horas
 */
public record MetricasResponse(long ventasHoy, BigDecimal totalVentasHoy, long comprasHoy, long devolucionesHoy,
                               long anulacionesHoy, long productosSinExistencias, BigDecimal valorInventario, long errores24h,
                               long erroresReportadosTaiga, long accesosDenegados24h, long loginsFallidos24h) {
}
