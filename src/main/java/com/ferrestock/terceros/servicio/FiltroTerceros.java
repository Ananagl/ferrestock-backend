/*
 * Nombre:                 FiltroTerceros.java
 * Descripción:            Criterios de búsqueda del listado de terceros (RF-TER-004).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.servicio;

import com.ferrestock.terceros.entidad.TipoPersona;

/**
 * @param texto       texto contenido en el nombre o el número de documento
 * @param rol         CLIENTE o PROVEEDOR
 * @param tipoPersona NATURAL o JURIDICA
 * @param activo      estado
 */
public record FiltroTerceros(String texto, RolTercero rol, TipoPersona tipoPersona, Boolean activo) {

    /** Rol comercial por el que se filtra. */
    public enum RolTercero {
        CLIENTE, PROVEEDOR
    }
}
