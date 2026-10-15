/*
 * Nombre:                 AuditoriaRepositorio.java
 * Descripción:            Acceso a datos de la auditoría con filtros dinámicos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.auditoria.repositorio;

import com.ferrestock.auditoria.entidad.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de {@link Auditoria}. Las consultas filtradas usan especificaciones (criterios con parámetros).
 */
public interface AuditoriaRepositorio extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
}
