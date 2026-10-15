/*
 * Nombre:                 IdiomaRequest.java
 * Descripción:            DTO de entrada del cambio de idioma preferido.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Idioma preferido del usuario (RNF-USA-01).
 *
 * @param idioma {@code es} o {@code en}
 */
public record IdiomaRequest(
    @NotBlank(message = "{validacion.obligatorio}") @Pattern(regexp = "es|en", message = "{validacion.idioma}")
    String idioma) {
}
