/*
 * Nombre:                 FiltroAuditoria.java
 * Descripción:            Criterios de búsqueda de la auditoría.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.auditoria.servicio;

import java.time.LocalDate;

import com.ferrestock.auditoria.entidad.AccionAuditoria;

/**
 * Filtros opcionales de la consulta de auditoría (RF-AUD-002).
 *
 * @param usuarioId usuario responsable
 * @param accion    código de acción
 * @param desde     fecha inicial inclusiva (zona America/Bogota)
 * @param hasta     fecha final inclusiva (zona America/Bogota)
 * @param texto     texto contenido en la descripción o el nombre de usuario
 */
public record FiltroAuditoria(Long usuarioId, AccionAuditoria accion, LocalDate desde, LocalDate hasta,
                              String texto) {
}
