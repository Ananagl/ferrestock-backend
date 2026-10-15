/*
 * Nombre:                 UsuarioActualizarRequest.java
 * Descripción:            DTO de entrada para modificar el nombre completo y el rol de un usuario.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios.dto.solicitud;

import com.ferrestock.usuarios.entidad.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Modificación de usuario. El nombre de usuario no cambia, porque identifica la cuenta en la auditoría.
 *
 * @param nombreCompleto nombre para mostrar
 * @param rol            ADMIN u OPERATIVO
 */
public record UsuarioActualizarRequest(
    @NotBlank(message = "{validacion.obligatorio}") @Size(min = 2, max = 120, message = "{validacion.longitud}")
    String nombreCompleto,
    @NotNull(message = "{validacion.obligatorio}")
    Rol rol) {
}
