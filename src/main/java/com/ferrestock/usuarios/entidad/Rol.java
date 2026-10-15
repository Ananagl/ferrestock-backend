/*
 * Nombre:                 Rol.java
 * Descripción:            Roles de autorización del sistema.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.usuarios.entidad;

/**
 * Roles confirmados en la Fase 1 (06_permisos_por_rol.md).
 */
public enum Rol {
    /** Administrador: ajustes, anulaciones, auditoría, usuarios. */
    ADMIN,
    /** Usuario operativo: operaciones comerciales e inventario. */
    OPERATIVO
}
