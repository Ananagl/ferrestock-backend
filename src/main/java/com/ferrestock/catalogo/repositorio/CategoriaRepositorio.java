/*
 * Nombre:                 CategoriaRepositorio.java
 * Descripción:            Acceso a datos de categorías.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.repositorio;

import java.util.List;
import java.util.Optional;

import com.ferrestock.catalogo.entidad.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link Categoria}.
 */
public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {

    /**
     * @param nombre nombre a buscar sin distinguir mayúsculas
     * @return la categoría, si existe
     */
    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    /**
     * @return todas las categorías ordenadas por nombre
     */
    List<Categoria> findAllByOrderByNombreAsc();

    /**
     * @param activa estado
     * @return categorías con ese estado ordenadas por nombre
     */
    List<Categoria> findByActivaOrderByNombreAsc(boolean activa);
}
