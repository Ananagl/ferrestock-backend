/*
 * Nombre:                 EstadoProductoRequest.java
 * Descripción:            DTO de entrada para activar o desactivar un producto.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.solicitud;

import com.ferrestock.catalogo.entidad.EstadoProducto;
import jakarta.validation.constraints.NotNull;

/**
 * @param estado ACTIVO o INACTIVO
 */
public record EstadoProductoRequest(@NotNull(message = "{validacion.obligatorio}") EstadoProducto estado) {
}
