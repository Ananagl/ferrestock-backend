/*
 * Nombre:                 TerceroActualizarRequest.java
 * Descripción:            DTO de entrada para modificar los datos de contacto y roles de un tercero (RF-TER-005).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.dto.solicitud;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Edición de tercero. El tipo de persona y el documento no se modifican: identifican al tercero en los documentos
 * comerciales (decisión de implementación de RF-TER-005, más estricta que la propuesta original).
 *
 * @param nombre      nombre completo o razón social
 * @param telefono    opcional
 * @param correo      opcional
 * @param direccion   opcional
 * @param esCliente   rol de cliente
 * @param esProveedor rol de proveedor
 */
public record TerceroActualizarRequest(
    @NotBlank(message = "{validacion.obligatorio}") @Size(min = 2, max = 150, message = "{validacion.longitud}")
    String nombre,
    @Pattern(regexp = "^[0-9 +()-]{7,20}$", message = "{tercero.telefono.formato}")
    String telefono,
    @Email(message = "{tercero.correo.formato}") @Size(max = 120, message = "{validacion.longitudMaxima}")
    String correo,
    @Size(max = 200, message = "{validacion.longitudMaxima}")
    String direccion,
    boolean esCliente,
    boolean esProveedor) {
}
