/*
 * Nombre:                 CambioContrasenaRequest.java
 * Descripción:            DTO de entrada del cambio de contraseña propio.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.dto.solicitud;

import com.ferrestock.compartido.validacion.ContrasenaSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contraseña (RF-AUT-007).
 *
 * @param actual       contraseña actual
 * @param nueva        nueva contraseña (política de RNF-SEG-01)
 * @param confirmacion repetición de la nueva contraseña
 */
public record CambioContrasenaRequest(
    @NotBlank(message = "{validacion.obligatorio}") @Size(max = 72, message = "{validacion.longitudMaxima}")
    String actual,
    @NotBlank(message = "{validacion.obligatorio}") @ContrasenaSegura
    String nueva,
    @NotBlank(message = "{validacion.obligatorio}") @Size(max = 72, message = "{validacion.longitudMaxima}")
    String confirmacion) {
}
