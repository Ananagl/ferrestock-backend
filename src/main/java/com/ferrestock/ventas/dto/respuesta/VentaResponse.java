/*
 * Nombre:                 VentaResponse.java
 * Descripción:            DTO de salida de una venta con pago y líneas (RF-VEN-005).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.ventas.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param id              venta
 * @param consecutivo     número del documento, por ejemplo {@code VEN-000001}
 * @param fecha           fecha y hora (UTC)
 * @param clienteId       cliente
 * @param clienteNombre   nombre del cliente
 * @param total           total en COP
 * @param medioPago       EFECTIVO
 * @param valorRecibido   efectivo recibido
 * @param cambio          cambio entregado
 * @param estado          REGISTRADA o ANULADA
 * @param motivoAnulacion motivo, si fue anulada
 * @param anuladaPor      usuario que la anuló
 * @param anuladaEn       fecha de anulación
 * @param usuario         usuario que la registró
 * @param lineas          líneas (solo en el detalle; nulo en el listado)
 */
public record VentaResponse(Long id, String consecutivo, Instant fecha, Long clienteId, String clienteNombre,
                            BigDecimal total, String medioPago, BigDecimal valorRecibido, BigDecimal cambio, String estado,
                            String motivoAnulacion, String anuladaPor, Instant anuladaEn, String usuario,
                            List<Linea> lineas) {

    /**
     * @param id               línea
     * @param productoId       producto
     * @param productoCodigo   código del producto
     * @param productoNombre   nombre del producto
     * @param cantidad         cantidad vendida
     * @param precioUnitario   precio vigente al vender
     * @param subtotal         subtotal
     * @param cantidadDevuelta cantidad ya devuelta
     * @param costoUnitario    CPP con que salió (solo ADMIN, DP-15)
     */
    public record Linea(Long id, Long productoId, String productoCodigo, String productoNombre, BigDecimal cantidad,
                        BigDecimal precioUnitario, BigDecimal subtotal, BigDecimal cantidadDevuelta,
                        BigDecimal costoUnitario) {
    }
}
