/*
 * Nombre:                 RegistroErrorRepositorio.java
 * Descripción:            Acceso a datos del registro de errores.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.13.0
 */
package com.ferrestock.monitoreo.repositorio;

import java.time.Instant;

import com.ferrestock.monitoreo.entidad.RegistroError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de errores registrados.
 */
public interface RegistroErrorRepositorio extends JpaRepository<RegistroError, Long> {

    /**
     * @param tipo  clase de la excepción
     * @param ruta  ruta de la petición
     * @param desde inicio de la ventana
     * @return cuántas veces ocurrió ese error en esa ruta desde la fecha
     */
    long countByTipoAndRutaAndFechaAfter(String tipo, String ruta, Instant desde);

    /**
     * @param tipo  clase de la excepción
     * @param ruta  ruta de la petición
     * @param desde inicio de la ventana
     * @return si ya se reportó a Taiga en la ventana (evita issues duplicados)
     */
    boolean existsByTipoAndRutaAndFechaAfterAndTaigaRefIsNotNull(String tipo, String ruta, Instant desde);

    /**
     * @param desde    fecha inicial
     * @param hasta    fecha final (exclusiva)
     * @param paginado página
     * @return errores del período, del más reciente al más antiguo
     */
    Page<RegistroError> findByFechaBetweenOrderByFechaDesc(Instant desde, Instant hasta, Pageable paginado);

    /**
     * @param desde fecha inicial
     * @return errores registrados desde la fecha
     */
    long countByFechaAfter(Instant desde);
}
