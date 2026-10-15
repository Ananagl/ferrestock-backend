/*
 * Nombre:                 CompraResponse.java
 * Descripción:            DTO de salida de una compra con sus líneas (RF-COM-004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param id              compra
 * @param consecutivo     número del documento, por ejemplo {@code COM-000001}
 * @param fecha           fecha y hora (UTC)
 * @param proveedorId     proveedor
 * @param proveedorNombre nombre del proveedor
 * @param observacion     observación
 * @param total           total en COP
 * @param estado          REGISTRADA o ANULADA
 * @param motivoAnulacion motivo, si fue anulada
 * @param anuladaPor      usuario que la anuló
 * @param anuladaEn       fecha de anulación
 * @param usuario         usuario que la registró
 * @param lineas          líneas (solo en el detalle; nulo en el listado)
 */
public record CompraResponse(Long id, String consecutivo, Instant fecha, Long proveedorId, String proveedorNombre,
                             String observacion, BigDecimal total, String estado, String motivoAnulacion,
                             String anuladaPor, Instant anuladaEn, String usuario, List<Linea> lineas) {

    /**
     * @param id             línea
     * @param productoId     producto
     * @param productoCodigo código del producto
     * @param productoNombre nombre del producto
     * @param cantidad       cantidad
     * @param costoUnitario  costo unitario
     * @param subtotal       subtotal
     */
    public record Linea(Long id, Long productoId, String productoCodigo, String productoNombre, BigDecimal cantidad,
                        BigDecimal costoUnitario, BigDecimal subtotal) {
    }
}
