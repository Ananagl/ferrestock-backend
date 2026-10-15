/*
 * Nombre:                 DevolucionResponse.java
 * Descripción:            DTO de salida de una devolución con sus líneas y el estado de validación (RF-DEV-005).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param id               devolución
 * @param consecutivo      número del documento, por ejemplo {@code DEV-000001}
 * @param fecha            fecha y hora (UTC)
 * @param clienteId        cliente
 * @param clienteNombre    nombre del cliente
 * @param ventaId          venta original, si la hay
 * @param ventaConsecutivo consecutivo de la venta original
 * @param motivo           motivo
 * @param validadaPor      usuario que validó las líneas
 * @param aceptadas        cantidad de líneas aceptadas
 * @param rechazadas       cantidad de líneas rechazadas
 * @param lineas           líneas (solo en el detalle; nulo en el listado)
 */
public record DevolucionResponse(Long id, String consecutivo, Instant fecha, Long clienteId, String clienteNombre,
                                 Long ventaId, String ventaConsecutivo, String motivo, String validadaPor, int aceptadas,
                                 int rechazadas, List<Linea> lineas) {

    /**
     * @param id             línea
     * @param productoId     producto
     * @param productoCodigo código del producto
     * @param productoNombre nombre del producto
     * @param cantidad       cantidad
     * @param resultado      ACEPTADA o RECHAZADA
     * @param costoUnitario  costo de reingreso (solo ADMIN y líneas aceptadas)
     */
    public record Linea(Long id, Long productoId, String productoCodigo, String productoNombre, BigDecimal cantidad,
                        String resultado, BigDecimal costoUnitario) {
    }
}
