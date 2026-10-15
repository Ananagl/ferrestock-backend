/*
 * Nombre:                 VentaRepositorio.java
 * Descripción:            Acceso a datos de las ventas.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.ventas.repositorio;

import java.util.Optional;

import com.ferrestock.ventas.entidad.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de ventas.
 */
public interface VentaRepositorio extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    /**
     * @param id venta
     * @return la venta con sus líneas
     */
    @EntityGraph(attributePaths = "lineas")
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> conLineas(@Param("id") Long id);

    /**
     * Bloquea la venta para anularla o para registrar una devolución sobre ella; así la cantidad devuelta y la
     * anulación no se pisan entre sí.
     *
     * @param id venta
     * @return la venta bloqueada ({@code SELECT … FOR UPDATE})
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> bloquear(@Param("id") Long id);
}
