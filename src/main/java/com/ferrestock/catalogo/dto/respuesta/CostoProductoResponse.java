/*
 * Nombre:                 CostoProductoResponse.java
 * Descripción:            DTO de salida con el costo promedio y la valorización de un producto (solo ADMIN, RF-COS-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.respuesta;

import java.math.BigDecimal;

/**
 * @param productoId      producto
 * @param costoPromedio   costo promedio ponderado vigente
 * @param existencias     existencias disponibles
 * @param valorInventario existencias × costo promedio, redondeado a 2 decimales
 */
public record CostoProductoResponse(Long productoId, BigDecimal costoPromedio, BigDecimal existencias,
                                    BigDecimal valorInventario) {
}
