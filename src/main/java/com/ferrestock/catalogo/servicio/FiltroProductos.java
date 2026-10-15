/*
 * Nombre:                 FiltroProductos.java
 * Descripción:            Criterios de búsqueda del listado de productos (RF-PRO-004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.servicio;

import com.ferrestock.catalogo.entidad.EstadoProducto;

/**
 * @param texto       texto contenido en el código o el nombre
 * @param categoriaId categoría
 * @param estado      ACTIVO o INACTIVO
 */
public record FiltroProductos(String texto, Long categoriaId, EstadoProducto estado) {
}
