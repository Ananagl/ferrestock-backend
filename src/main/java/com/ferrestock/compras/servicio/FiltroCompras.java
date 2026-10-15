/*
 * Nombre:                 FiltroCompras.java
 * Descripción:            Criterios de búsqueda de compras (RF-COM-004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.servicio;

import java.time.LocalDate;

import com.ferrestock.compartido.documentos.EstadoDocumento;

/**
 * @param proveedorId proveedor
 * @param desde       fecha inicial inclusiva (zona America/Bogota)
 * @param hasta       fecha final inclusiva (zona America/Bogota)
 * @param estado      REGISTRADA o ANULADA
 */
public record FiltroCompras(Long proveedorId, LocalDate desde, LocalDate hasta, EstadoDocumento estado) {
}
