/*
 * Nombre:                 EstadoUsuarioRequest.java
 * Descripción:            DTO de entrada para activar o desactivar un usuario.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios.dto.solicitud;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio de estado (eliminación lógica: los usuarios nunca se borran).
 *
 * @param activo nuevo estado
 */
public record EstadoUsuarioRequest(@NotNull(message = "{validacion.obligatorio}") Boolean activo) {
}
