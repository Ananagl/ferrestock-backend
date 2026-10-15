/*
 * Nombre:                 TipoDocumentoComercial.java
 * Descripción:            Tipos de documento comercial que originan movimientos de inventario y llevan consecutivo.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.compartido.documentos;

/**
 * Documentos con consecutivo propio (RF-DOC-001) y origen de los movimientos del kardex.
 */
public enum TipoDocumentoComercial {
    COMPRA, VENTA, DEVOLUCION, AJUSTE
}
