/*
 * Nombre:                 ValorizacionResponse.java
 * Descripción:            DTO de salida con el valor del inventario total y por categoría (solo ADMIN, RF-COS-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.dto.respuesta;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param total      valor total del inventario
 * @param categorias valor por categoría
 */
public record ValorizacionResponse(BigDecimal total, List<Categoria> categorias) {

    /**
     * @param categoriaId id
     * @param nombre      nombre
     * @param productos   productos con existencias
     * @param valor       valor del inventario de la categoría
     */
    public record Categoria(Long categoriaId, String nombre, long productos, BigDecimal valor) {
    }
}
