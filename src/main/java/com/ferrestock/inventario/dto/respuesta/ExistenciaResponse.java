/*
 * Nombre:                 ExistenciaResponse.java
 * Descripción:            DTO de salida de existencias por producto; el costo y el valor solo se llenan para el ADMIN (DP-15).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.dto.respuesta;

import java.math.BigDecimal;

/**
 * @param productoId      producto
 * @param codigo          código
 * @param nombre          nombre
 * @param categoria       nombre de la categoría
 * @param unidadMedida    unidad
 * @param estado          ACTIVO o INACTIVO
 * @param existencias     existencias disponibles
 * @param costoPromedio   CPP vigente (solo ADMIN; nulo y omitido para OPERATIVO)
 * @param valorInventario existencias × CPP (solo ADMIN)
 */
public record ExistenciaResponse(Long productoId, String codigo, String nombre, String categoria, String unidadMedida,
                                 String estado, BigDecimal existencias, BigDecimal costoPromedio,
                                 BigDecimal valorInventario) {
}
