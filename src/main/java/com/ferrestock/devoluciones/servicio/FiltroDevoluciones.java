/*
 * Nombre:                 FiltroDevoluciones.java
 * Descripción:            Criterios de búsqueda de devoluciones (RF-DEV-005).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.servicio;

import java.time.LocalDate;

/**
 * @param clienteId cliente
 * @param ventaId   venta original
 * @param desde     fecha inicial inclusiva (zona America/Bogota)
 * @param hasta     fecha final inclusiva (zona America/Bogota)
 */
public record FiltroDevoluciones(Long clienteId, Long ventaId, LocalDate desde, LocalDate hasta) {
}
