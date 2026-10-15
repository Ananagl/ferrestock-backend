/*
 * Nombre:                 UnidadMedida.java
 * Descripción:            Unidades de medida permitidas para los productos (DP-05).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.catalogo.entidad;

/**
 * Lista cerrada de unidades. Si una unidad admite cantidades fraccionarias (p. ej. metros de cable) lo decide la
 * marca {@code permiteFraccion} de cada producto, no la unidad.
 */
public enum UnidadMedida {
    UNIDAD, METRO, KILOGRAMO, LITRO, GALON, CAJA, PAQUETE, ROLLO
}
