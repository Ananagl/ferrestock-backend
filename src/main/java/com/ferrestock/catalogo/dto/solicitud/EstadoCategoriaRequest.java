/*
 * Nombre:                 EstadoCategoriaRequest.java
 * Descripción:            DTO de entrada para activar o desactivar una categoría.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.solicitud;

import jakarta.validation.constraints.NotNull;

/**
 * @param activa nuevo estado
 */
public record EstadoCategoriaRequest(@NotNull(message = "{validacion.obligatorio}") Boolean activa) {
}
