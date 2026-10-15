/*
 * Nombre:                 TerceroCrearRequest.java
 * Descripción:            DTO de entrada para registrar un cliente o proveedor (RF-TER-001, RF-TER-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.dto.solicitud;

import com.ferrestock.terceros.entidad.TipoDocumento;
import com.ferrestock.terceros.entidad.TipoPersona;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de tercero. Las reglas que combinan campos (persona/documento, dígito de verificación, al menos un rol) las
 * aplica el servicio, que responde con el campo afectado.
 *
 * @param tipoPersona        NATURAL o JURIDICA
 * @param tipoDocumento      CC, CE o NIT (jurídica: solo NIT)
 * @param numeroDocumento    5 a 15 dígitos, sin puntos ni dígito de verificación
 * @param digitoVerificacion obligatorio si es NIT
 * @param nombre             nombre completo o razón social
 * @param telefono           opcional
 * @param correo             opcional
 * @param direccion          opcional
 * @param esCliente          rol de cliente
 * @param esProveedor        rol de proveedor
 */
public record TerceroCrearRequest(
    @NotNull(message = "{validacion.obligatorio}") TipoPersona tipoPersona,
    @NotNull(message = "{validacion.obligatorio}") TipoDocumento tipoDocumento,
    @NotBlank(message = "{validacion.obligatorio}")
    @Pattern(regexp = "^[0-9]{5,15}$", message = "{tercero.numero.formato}")
    String numeroDocumento,
    @Pattern(regexp = "^[0-9]$", message = "{tercero.dv.formato}")
    String digitoVerificacion,
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
