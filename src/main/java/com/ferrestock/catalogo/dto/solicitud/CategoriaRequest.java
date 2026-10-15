/*
 * Nombre:                 CategoriaRequest.java
 * Descripción:            DTO de entrada para crear o renombrar una categoría.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.solicitud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param nombre nombre único (sin distinguir mayúsculas), de 2 a 60 caracteres
 */
public record CategoriaRequest(
    @NotBlank(message = "{validacion.obligatorio}") @Size(min = 2, max = 60, message = "{validacion.longitud}")
    String nombre) {
}
