/*
 * Nombre:                 ComprobanteResponse.java
 * Descripción:            DTO de salida de un comprobante imprimible de compra, venta, devolución o ajuste (RF-DOC-002 a 004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.10.0
 */
package com.ferrestock.comprobantes.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Comprobante con un formato común para los cuatro tipos de documento (DP-12). Los campos que no aplican a un tipo
 * llegan nulos (por ejemplo, el pago solo existe en las ventas).
 *
 * @param negocio       datos del negocio para el encabezado
 * @param tipo          COMPRA, VENTA, DEVOLUCION o AJUSTE
 * @param consecutivo   número del documento
 * @param fecha         fecha y hora (UTC)
 * @param estado        REGISTRADA o ANULADA
 * @param tercero       cliente o proveedor (nulo en los ajustes)
 * @param usuario       usuario que registró el documento
 * @param referencia    venta de origen de una devolución, u observación de la compra
 * @param motivo        motivo (devoluciones, ajustes y anulaciones)
 * @param lineas        líneas del documento
 * @param total         total en COP (compras y ventas)
 * @param valorRecibido efectivo recibido (ventas)
 * @param cambio        cambio entregado (ventas)
 * @param leyenda       "Documento simulado — sin validez fiscal — no transmitido a la DIAN" en el idioma de la petición
 */
public record ComprobanteResponse(Negocio negocio, String tipo, String consecutivo, Instant fecha, String estado,
                                  Tercero tercero, String usuario, String referencia, String motivo, List<Linea> lineas,
                                  BigDecimal total, BigDecimal valorRecibido, BigDecimal cambio, String leyenda) {

    /**
     * @param nombre    razón social
     * @param nit       NIT
     * @param direccion dirección
     */
    public record Negocio(String nombre, String nit, String direccion) {
    }

    /**
     * @param nombre    nombre o razón social
     * @param documento tipo y número de documento
     */
    public record Tercero(String nombre, String documento) {
    }

    /**
     * @param codigo        código del producto
     * @param descripcion   nombre del producto
     * @param cantidad      cantidad
     * @param valorUnitario precio, costo o nulo
     * @param subtotal      subtotal o nulo
     * @param detalle       resultado de la línea (devoluciones) o sentido del ajuste
     */
    public record Linea(String codigo, String descripcion, BigDecimal cantidad, BigDecimal valorUnitario,
                        BigDecimal subtotal, String detalle) {
    }
}
