/*
 * Nombre:                 FiltroVentas.java
 * Descripción:            Criterios de búsqueda de ventas (RF-VEN-005).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.ventas.servicio;

import java.time.LocalDate;

import com.ferrestock.compartido.documentos.EstadoDocumento;

/**
 * @param clienteId cliente
 * @param desde     fecha inicial inclusiva (zona America/Bogota)
 * @param hasta     fecha final inclusiva (zona America/Bogota)
 * @param estado    REGISTRADA o ANULADA
 */
public record FiltroVentas(Long clienteId, LocalDate desde, LocalDate hasta, EstadoDocumento estado) {
}
