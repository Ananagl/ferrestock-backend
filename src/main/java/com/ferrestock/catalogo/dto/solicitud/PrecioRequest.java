/*
 * Nombre:                 PrecioRequest.java
 * Descripción:            DTO de entrada para modificar el precio de venta (RF-PRE-001).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.solicitud;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * @param precioVenta nuevo precio en COP, mayor que cero, con hasta 2 decimales
 */
public record PrecioRequest(
    @NotNull(message = "{validacion.obligatorio}")
    @DecimalMin(value = "0", inclusive = false, message = "{producto.precio.positivo}")
    @Digits(integer = 12, fraction = 2, message = "{producto.precio.formato}")
    BigDecimal precioVenta) {
}
