/*
 * Nombre:                 ProductoRepositorio.java
 * Descripción:            Acceso a datos de productos con filtros dinámicos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.repositorio;

import java.util.Optional;

import com.ferrestock.catalogo.entidad.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de {@link Producto}.
 */
public interface ProductoRepositorio extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    /**
     * @param codigo código interno, sin distinguir mayúsculas
     * @return el producto, si existe
     */
    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    /**
     * Listado filtrado que trae la categoría en la misma consulta (evita N+1 consultas).
     *
     * @param especificacion filtros
     * @param pagina         página solicitada
     * @return página de productos
     */
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Producto> findAll(Specification<Producto> especificacion, Pageable pagina);
}
