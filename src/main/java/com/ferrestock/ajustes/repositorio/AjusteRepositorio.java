/*
 * Nombre:                 AjusteRepositorio.java
 * Descripción:            Acceso a datos de los ajustes de inventario.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.repositorio;

import com.ferrestock.ajustes.entidad.Ajuste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de ajustes. Solo se insertan y se consultan.
 */
public interface AjusteRepositorio extends JpaRepository<Ajuste, Long>, JpaSpecificationExecutor<Ajuste> {
}
