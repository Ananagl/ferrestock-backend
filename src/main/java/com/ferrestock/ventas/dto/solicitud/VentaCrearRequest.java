/*
 * Nombre:                 VentaCrearRequest.java
 * Descripción:            DTO de entrada para confirmar una venta con su pago en efectivo (04_api_rest.md, VentaRequest).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.ventas.dto.solicitud;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Nueva venta. El precio no viaja: el servidor usa el precio vigente de cada producto, para que no se pueda
 * manipular desde el cliente.
 *
 * @param clienteId cliente registrado y activo (CA-VEN-04)
 * @param lineas    de 1 a 100 líneas, sin productos repetidos
 * @param pago      pago en efectivo
 */
public record VentaCrearRequest(
    @NotNull(message = "{validacion.obligatorio}") Long clienteId,
    @NotEmpty(message = "{validacion.obligatorio}") @Size(max = 100, message = "{documento.lineas.maximo}")
    List<@Valid Linea> lineas,
    @NotNull(message = "{validacion.obligatorio}") @Valid Pago pago) {

    /**
     * @param productoId producto activo
     * @param cantidad   cantidad mayor que cero
     */
    public record Linea(
        @NotNull(message = "{validacion.obligatorio}") Long productoId,
        @NotNull(message = "{validacion.obligatorio}") BigDecimal cantidad) {
    }

    /**
     * @param medio         solo EFECTIVO (RF-PAG-002)
     * @param valorRecibido efectivo recibido; debe cubrir el total
     */
    public record Pago(
        @NotBlank(message = "{validacion.obligatorio}") String medio,
        @NotNull(message = "{validacion.obligatorio}")
        @DecimalMin(value = "0", message = "{documento.valor.formato}")
        @Digits(integer = 12, fraction = 2, message = "{documento.valor.formato}")
        BigDecimal valorRecibido) {
    }
}
