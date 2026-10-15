/*
 * Nombre:                 RestablecimientoRequest.java
 * Descripción:            DTO de entrada para asignar una contraseña temporal a otro usuario.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios.dto.solicitud;

import com.ferrestock.compartido.validacion.ContrasenaSegura;
import jakarta.validation.constraints.NotBlank;

/**
 * Restablecimiento de contraseña por el administrador.
 *
 * @param contrasenaTemporal nueva contraseña temporal que cumple la política
 */
public record RestablecimientoRequest(
    @NotBlank(message = "{validacion.obligatorio}") @ContrasenaSegura String contrasenaTemporal) {
}
