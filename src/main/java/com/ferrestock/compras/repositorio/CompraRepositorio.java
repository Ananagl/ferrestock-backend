/*
 * Nombre:                 CompraRepositorio.java
 * Descripción:            Acceso a datos de las compras.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.repositorio;

import java.util.Optional;

import com.ferrestock.compras.entidad.Compra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de compras.
 */
public interface CompraRepositorio extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    /**
     * @param id compra
     * @return la compra con sus líneas
     */
    @EntityGraph(attributePaths = "lineas")
    @Query("select c from Compra c where c.id = :id")
    Optional<Compra> conLineas(@Param("id") Long id);

    /**
     * Bloquea la compra para anularla, de modo que dos anulaciones simultáneas no se ejecuten las dos.
     *
     * @param id compra
     * @return la compra bloqueada ({@code SELECT … FOR UPDATE})
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compra c where c.id = :id")
    Optional<Compra> bloquear(@Param("id") Long id);
}
