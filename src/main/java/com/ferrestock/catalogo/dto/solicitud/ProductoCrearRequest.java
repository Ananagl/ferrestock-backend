/*
 * Nombre:                 ProductoCrearRequest.java
 * Descripción:            DTO de entrada para registrar un producto (RF-PRO-001).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.solicitud;

import java.math.BigDecimal;

import com.ferrestock.catalogo.entidad.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de producto. No incluye existencias ni costo: los calcula el sistema.
 *
 * @param codigo          código interno único (se guarda en mayúsculas)
 * @param nombre          nombre comercial
 * @param descripcion     descripción opcional
 * @param categoriaId     categoría activa
 * @param unidadMedida    unidad de medida
 * @param permiteFraccion si admite cantidades con decimales
 * @param precioVenta     precio de venta en COP, mayor que cero
 */
public record ProductoCrearRequest(
    @NotBlank(message = "{validacion.obligatorio}")
    @Pattern(regexp = "^[A-Za-z0-9-]{2,30}$", message = "{producto.codigo.formato}")
    String codigo,
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
    @DecimalMin(value = "0", inclusive = false, message = "{producto.precio.positivo}")
    @Digits(integer = 12, fraction = 2, message = "{producto.precio.formato}")
    BigDecimal precioVenta) {
}
