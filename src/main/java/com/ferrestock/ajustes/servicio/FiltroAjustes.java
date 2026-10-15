/*
 * Nombre:                 FiltroAjustes.java
 * Descripción:            Criterios de búsqueda del historial de ajustes (RF-AJU-004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.servicio;

import java.time.LocalDate;

import com.ferrestock.ajustes.entidad.TipoAjuste;

/**
 * @param productoId producto
 * @param desde      fecha inicial inclusiva (zona America/Bogota)
 * @param hasta      fecha final inclusiva (zona America/Bogota)
 * @param tipo       ENTRADA o SALIDA
 */
public record FiltroAjustes(Long productoId, LocalDate desde, LocalDate hasta, TipoAjuste tipo) {
}
