/*
 * Nombre:                 TipoMovimiento.java
 * Descripción:            Tipos de movimiento del kardex con su naturaleza (03_inventario_y_costos.md, sección 2).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.inventario.entidad;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de movimiento. Cada uno tiene una naturaleza fija, para que una compra nunca se registre como salida.
 */
@Getter
@RequiredArgsConstructor
public enum TipoMovimiento {
    COMPRA(Naturaleza.ENTRADA),
    VENTA(Naturaleza.SALIDA),
    DEVOLUCION(Naturaleza.ENTRADA),
    AJUSTE_ENTRADA(Naturaleza.ENTRADA),
    AJUSTE_SALIDA(Naturaleza.SALIDA),
    ANULACION_COMPRA(Naturaleza.SALIDA),
    ANULACION_VENTA(Naturaleza.ENTRADA);

    /** Entrada o salida. */
    private final Naturaleza naturaleza;
}
