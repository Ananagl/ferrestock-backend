/*
 * Nombre:                 EstadoTerceroRequest.java
 * Descripción:            DTO de entrada para activar o desactivar un tercero.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.dto.solicitud;

import jakarta.validation.constraints.NotNull;

/**
 * @param activo nuevo estado
 */
public record EstadoTerceroRequest(@NotNull(message = "{validacion.obligatorio}") Boolean activo) {
}
