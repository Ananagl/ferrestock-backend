/*
 * Nombre:                 TerceroRepositorio.java
 * Descripción:            Acceso a datos de terceros con filtros dinámicos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.repositorio;

import java.util.Optional;

import com.ferrestock.terceros.entidad.Tercero;
import com.ferrestock.terceros.entidad.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de {@link Tercero}.
 */
public interface TerceroRepositorio extends JpaRepository<Tercero, Long>, JpaSpecificationExecutor<Tercero> {

    /**
     * @param tipoDocumento   tipo de documento
     * @param numeroDocumento número sin dígito de verificación
     * @return el tercero, si existe (clave única RNP-02)
     */
    Optional<Tercero> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
