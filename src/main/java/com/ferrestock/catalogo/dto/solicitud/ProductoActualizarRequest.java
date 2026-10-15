/*
 * Nombre:                 ProductoActualizarRequest.java
 * Descripción:            DTO de entrada para modificar los datos descriptivos de un producto (RF-PRO-005).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.solicitud;

import com.ferrestock.catalogo.entidad.UnidadMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Edición de producto. El código no cambia (identifica el producto en documentos) y el precio se cambia por su
 * propio endpoint para auditarlo (RF-PRE-002).
 *
 * @param nombre          nombre comercial
 * @param descripcion     descripción opcional
 * @param categoriaId     categoría activa
 * @param unidadMedida    unidad de medida
 * @param permiteFraccion si admite cantidades con decimales
 * @param version         versión leída por el cliente (bloqueo optimista)
 */
public record ProductoActualizarRequest(
    @NotBlank(message = "{validacion.obligatorio}") @Size(min = 2, max = 120, message = "{validacion.longitud}")
    String nombre,
    @Size(max = 500, message = "{validacion.longitudMaxima}")
    String descripcion,
    @NotNull(message = "{validacion.obligatorio}")
    Long categoriaId,
    @NotNull(message = "{validacion.obligatorio}")
    UnidadMedida unidadMedida,
    boolean permiteFraccion,
    @NotNull(message = "{validacion.obligatorio}")
    Long version) {
}
