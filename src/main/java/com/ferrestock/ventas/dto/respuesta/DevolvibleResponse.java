/*
 * Nombre:                 DevolvibleResponse.java
 * Descripción:            DTO de salida con lo que queda por devolver de cada línea de una venta (04_api_rest.md, 4.8).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.ventas.dto.respuesta;

import java.math.BigDecimal;

/**
 * @param ventaLineaId    línea de venta
 * @param productoId      producto
 * @param productoCodigo  código del producto
 * @param productoNombre  nombre del producto
 * @param permiteFraccion si el producto admite cantidades con decimales
 * @param vendida         cantidad vendida
 * @param devuelta        cantidad ya devuelta (aceptada)
 * @param disponible      cantidad que aún se puede devolver
 */
public record DevolvibleResponse(Long ventaLineaId, Long productoId, String productoCodigo, String productoNombre,
                                 boolean permiteFraccion, BigDecimal vendida, BigDecimal devuelta, BigDecimal disponible) {
}
