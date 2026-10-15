/*
 * Nombre:                 ResultadoLinea.java
 * Descripción:            Resultado de la validación de una línea de devolución (RF-DEV-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.entidad;

/**
 * Solo las líneas aceptadas reingresan al inventario (RF-DEV-003).
 */
public enum ResultadoLinea {
    /** El producto se recibe y vuelve al inventario. */
    ACEPTADA,
    /** El producto no se recibe; la línea queda como evidencia de la solicitud. */
    RECHAZADA
}
