/*
 * Nombre:                 UsuarioCrearRequest.java
 * Descripción:            DTO de entrada para crear un usuario (RF-AUT-006).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios.dto.solicitud;

import com.ferrestock.compartido.validacion.ContrasenaSegura;
import com.ferrestock.usuarios.entidad.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de usuario. El nombre se guarda en minúsculas y la contraseña es temporal: se debe cambiar al ingresar.
 *
 * @param nombreUsuario      3 a 50 caracteres: letras minúsculas, números, punto, guion y guion bajo
 * @param nombreCompleto     nombre para mostrar
 * @param rol                ADMIN u OPERATIVO
 * @param contrasenaTemporal contraseña inicial que cumple la política
 */
public record UsuarioCrearRequest(
    @NotBlank(message = "{validacion.obligatorio}")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "{usuario.nombreUsuario.formato}")
    String nombreUsuario,
    @NotBlank(message = "{validacion.obligatorio}") @Size(min = 2, max = 120, message = "{validacion.longitud}")
    String nombreCompleto,
    @NotNull(message = "{validacion.obligatorio}")
    Rol rol,
    @NotBlank(message = "{validacion.obligatorio}") @ContrasenaSegura
    String contrasenaTemporal) {
}
