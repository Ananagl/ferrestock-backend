/*
 * Nombre:                 DevolucionRepositorio.java
 * Descripción:            Acceso a datos de las devoluciones.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.repositorio;

import java.util.Optional;

import com.ferrestock.devoluciones.entidad.Devolucion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de devoluciones (solo inserción y lectura).
 */
public interface DevolucionRepositorio extends JpaRepository<Devolucion, Long>, JpaSpecificationExecutor<Devolucion> {

    /**
     * @param id devolución
     * @return la devolución con sus líneas
     */
    @EntityGraph(attributePaths = "lineas")
    @Query("select d from Devolucion d where d.id = :id")
    Optional<Devolucion> conLineas(@Param("id") Long id);
}
