/*
 * Nombre:                 CompraCrearRequest.java
 * Descripción:            DTO de entrada para registrar una compra con sus líneas (RF-COM-001).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.dto.solicitud;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Nueva compra. Las reglas que dependen de la base (proveedor activo, producto activo y repetido, cantidad según el
 * producto) las aplica el servicio, que responde con el campo de la línea que falló (RF-COM-003).
 *
 * @param proveedorId proveedor activo
 * @param observacion texto opcional, por ejemplo el número de factura del proveedor
 * @param lineas      de 1 a 100 líneas, sin productos repetidos
 */
public record CompraCrearRequest(
    @NotNull(message = "{validacion.obligatorio}") Long proveedorId,
    @Size(max = 300, message = "{validacion.longitudMaxima}") String observacion,
    @NotEmpty(message = "{validacion.obligatorio}") @Size(max = 100, message = "{documento.lineas.maximo}")
    List<@Valid Linea> lineas) {

    /**
     * @param productoId    producto activo
     * @param cantidad      cantidad mayor que cero
     * @param costoUnitario costo unitario en COP, mayor o igual que cero
     */
    public record Linea(
        @NotNull(message = "{validacion.obligatorio}") Long productoId,
        @NotNull(message = "{validacion.obligatorio}") BigDecimal cantidad,
        @NotNull(message = "{validacion.obligatorio}")
        @DecimalMin(value = "0", message = "{documento.costo.formato}")
        @Digits(integer = 12, fraction = 4, message = "{documento.costo.formato}")
        BigDecimal costoUnitario) {
    }
}
