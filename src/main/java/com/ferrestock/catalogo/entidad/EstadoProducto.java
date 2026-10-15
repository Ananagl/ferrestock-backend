/*
 * Nombre:                 EstadoProducto.java
 * Descripción:            Estados de un producto: activo o inactivo (desactivación lógica, RN-02).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.entidad;

/**
 * Estado del producto. Un producto INACTIVO conserva su historial y no admite nuevas operaciones (RF-PRO-008).
 */
public enum EstadoProducto {
    ACTIVO, INACTIVO
}
