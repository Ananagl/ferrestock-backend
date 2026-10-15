/*
 * Nombre:                 FiltroKardex.java
 * Descripción:            Criterios de búsqueda del kardex de un producto (RF-INV-003).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.servicio;

import java.time.LocalDate;

import com.ferrestock.inventario.entidad.TipoMovimiento;

/**
 * @param desde fecha inicial inclusiva (zona America/Bogota)
 * @param hasta fecha final inclusiva (zona America/Bogota)
 * @param tipo  tipo de movimiento
 */
public record FiltroKardex(LocalDate desde, LocalDate hasta, TipoMovimiento tipo) {
}
