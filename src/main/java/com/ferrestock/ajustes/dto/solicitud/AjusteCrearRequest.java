/*
 * Nombre:                 AjusteCrearRequest.java
 * Descripción:            DTO de entrada para registrar un ajuste de inventario (RF-AJU-001).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.dto.solicitud;

import java.math.BigDecimal;

import com.ferrestock.ajustes.entidad.TipoAjuste;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nuevo ajuste. La cantidad (mayor que cero, fracciones según el producto) y la obligatoriedad del costo en las
 * entradas las valida el servicio, que responde con el campo afectado.
 *
 * @param productoId    producto activo
 * @param tipo          ENTRADA o SALIDA
 * @param cantidad      cantidad, hasta 3 decimales
 * @param costoUnitario costo unitario en COP, obligatorio en las entradas; se ignora en las salidas
 * @param motivo        motivo documentado, de 5 a 300 caracteres sin contar espacios al inicio y al final
 */
public record AjusteCrearRequest(
    @NotNull(message = "{validacion.obligatorio}") Long productoId,
    @NotNull(message = "{validacion.obligatorio}") TipoAjuste tipo,
    @NotNull(message = "{validacion.obligatorio}") BigDecimal cantidad,
    @DecimalMin(value = "0", message = "{ajuste.costo.formato}")
    @Digits(integer = 12, fraction = 4, message = "{ajuste.costo.formato}")
    BigDecimal costoUnitario,
    @NotBlank(message = "{validacion.obligatorio}")
    @Size(max = 300, message = "{validacion.longitudMaxima}")
    @Pattern(regexp = "^\\s*\\S[\\s\\S]{3,}\\S\\s*$", message = "{ajuste.motivo.longitud}")
    String motivo) {
}
