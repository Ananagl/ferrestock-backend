/*
 * Nombre:                 ProductoResponse.java
 * Descripción:            DTO de salida de un producto, sin el costo (que solo ve el ADMIN, DP-15).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.respuesta;

import java.math.BigDecimal;
import java.time.Instant;

import com.ferrestock.catalogo.entidad.Producto;

/**
 * Producto expuesto por la API. No incluye el costo promedio: se consulta en {@code /productos/{id}/costo}.
 *
 * @param id              identificador
 * @param codigo          código interno
 * @param nombre          nombre
 * @param descripcion     descripción
 * @param categoria       categoría (id y nombre)
 * @param unidadMedida    unidad de medida
 * @param permiteFraccion si admite cantidades con decimales
 * @param precioVenta     precio vigente en COP
 * @param existencias     existencias disponibles
 * @param estado          ACTIVO o INACTIVO
 * @param version         versión para el bloqueo optimista
 * @param creadoEn        fecha de alta
 * @param actualizadoEn   fecha de la última modificación
 */
public record ProductoResponse(Long id, String codigo, String nombre, String descripcion, CategoriaResponse categoria,
                               String unidadMedida, boolean permiteFraccion, BigDecimal precioVenta,
                               BigDecimal existencias, String estado, Long version, Instant creadoEn,
                               Instant actualizadoEn) {

    /**
     * @param producto entidad (con la categoría accesible)
     * @return DTO equivalente
     */
    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(producto.getId(), producto.getCodigo(), producto.getNombre(),
            producto.getDescripcion(), CategoriaResponse.desde(producto.getCategoria()),
            producto.getUnidadMedida().name(), producto.isPermiteFraccion(), producto.getPrecioVenta(),
            producto.getExistencias(), producto.getEstado().name(), producto.getVersion(), producto.getCreadoEn(),
            producto.getActualizadoEn());
    }
}
