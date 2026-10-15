/*
 * Nombre:                 AccionAuditoria.java
 * Descripción:            Códigos estables de las acciones auditadas.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.auditoria.entidad;

/**
 * Acciones registradas en la auditoría. Cada incremento agrega las de su módulo.
 */
public enum AccionAuditoria {
    /** Inicio de sesión correcto. */
    LOGIN_EXITOSO,
    /** Intento de inicio de sesión fallido. */
    LOGIN_FALLIDO,
    /** La cuenta se bloqueó por intentos fallidos. */
    CUENTA_BLOQUEADA,
    /** Cierre de sesión. */
    LOGOUT,
    /** Cambio de contraseña propio. */
    CONTRASENA_CAMBIADA,
    /** Intento de usar un endpoint sin el rol requerido. */
    ACCESO_DENEGADO
}
