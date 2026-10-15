/*
 * Nombre:                 DevolucionCrearRequest.java
 * Descripción:            DTO de entrada para registrar y validar una devolución de cliente (RF-DEV-001 a 004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.dto.solicitud;

import java.math.BigDecimal;
import java.util.List;

import com.ferrestock.devoluciones.entidad.ResultadoLinea;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nueva devolución. Si se indica la venta, cada producto debe estar en ella y la cantidad aceptada acumulada no puede
 * superar la vendida (RNP-04); lo verifica el servicio.
 *
 * @param clienteId cliente registrado
 * @param ventaId   venta original (opcional)
 * @param motivo    motivo obligatorio, de 5 a 300 caracteres
 * @param lineas    de 1 a 100 líneas, sin productos repetidos
 */
public record DevolucionCrearRequest(
    @NotNull(message = "{validacion.obligatorio}") Long clienteId,
    Long ventaId,
    @NotBlank(message = "{validacion.obligatorio}")
    @Size(max = 300, message = "{validacion.longitudMaxima}")
    @Pattern(regexp = "^\\s*\\S[\\s\\S]{3,}\\S\\s*$", message = "{documento.motivo.longitud}")
    String motivo,
    @NotEmpty(message = "{validacion.obligatorio}") @Size(max = 100, message = "{documento.lineas.maximo}")
    List<@Valid Linea> lineas) {

    /**
     * @param productoId producto (se aceptan productos inactivos, DP-09)
     * @param cantidad   cantidad devuelta
     * @param resultado  ACEPTADA o RECHAZADA, según la validación del responsable
     */
    public record Linea(
        @NotNull(message = "{validacion.obligatorio}") Long productoId,
        @NotNull(message = "{validacion.obligatorio}") BigDecimal cantidad,
        @NotNull(message = "{validacion.obligatorio}") ResultadoLinea resultado) {
    }
}
