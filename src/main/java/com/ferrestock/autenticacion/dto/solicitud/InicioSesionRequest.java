/*
 * Nombre:                 InicioSesionRequest.java
 * Descripción:            DTO de entrada del inicio de sesión.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales del formulario de ingreso (RF-AUT-001, RF-AUT-002).
 *
 * @param usuario    nombre de usuario
 * @param contrasena contraseña
 * @param recordarme si se pide sesión persistente
 */
public record InicioSesionRequest(
    @NotBlank(message = "{auth.usuario.obligatorio}") @Size(max = 50, message = "{validacion.longitudMaxima}")
    String usuario,
    @NotBlank(message = "{auth.contrasena.obligatoria}") @Size(max = 72, message = "{validacion.longitudMaxima}")
    String contrasena,
    boolean recordarme) {
}
