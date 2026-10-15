/*
 * Nombre:                 AnulacionRequest.java
 * Descripción:            DTO de entrada para anular una compra o una venta (DP-07).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compartido.documentos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param motivo motivo de la anulación, de 5 a 300 caracteres sin contar espacios al inicio y al final
 */
public record AnulacionRequest(
    @NotBlank(message = "{validacion.obligatorio}")
    @Size(max = 300, message = "{validacion.longitudMaxima}")
    @Pattern(regexp = "^\\s*\\S[\\s\\S]{3,}\\S\\s*$", message = "{documento.motivo.longitud}")
    String motivo) {
}
