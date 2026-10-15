/*
 * Nombre:                 ConsecutivoRepositorio.java
 * Descripción:            Acceso con bloqueo a los contadores de consecutivos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.comprobantes.repositorio;

import java.util.Optional;

import com.ferrestock.comprobantes.entidad.Consecutivo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Consecutivo}.
 */
public interface ConsecutivoRepositorio extends JpaRepository<Consecutivo, String> {

    /**
     * @param tipo tipo de documento
     * @return el contador bloqueado ({@code SELECT … FOR UPDATE})
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consecutivo c where c.tipoDocumento = :tipo")
    Optional<Consecutivo> bloquear(@Param("tipo") String tipo);
}
