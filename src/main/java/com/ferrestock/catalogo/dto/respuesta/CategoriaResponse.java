/*
 * Nombre:                 CategoriaResponse.java
 * Descripción:            DTO de salida de una categoría.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.dto.respuesta;

import com.ferrestock.catalogo.entidad.Categoria;

/**
 * @param id     identificador
 * @param nombre nombre
 * @param activa estado
 */
public record CategoriaResponse(Long id, String nombre, boolean activa) {

    /**
     * @param categoria entidad
     * @return DTO equivalente
     */
    public static CategoriaResponse desde(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.isActiva());
    }
}
