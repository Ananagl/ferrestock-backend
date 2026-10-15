/*
 * Nombre:                 EstadoDocumento.java
 * Descripción:            Estado de un documento comercial que admite anulación (compras y ventas).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compartido.documentos;

/**
 * Los documentos nunca se borran: se anulan conservando el consecutivo (RNP-05, RNP-07).
 */
public enum EstadoDocumento {
    /** Documento vigente. */
    REGISTRADA,
    /** Documento anulado por el Administrador, con motivo. */
    ANULADA
}
