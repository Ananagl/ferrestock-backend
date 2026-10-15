/*
 * Nombre:                 TipoAjuste.java
 * Descripción:            Sentido de un ajuste de inventario y el movimiento de kardex que genera.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.entidad;

import com.ferrestock.inventario.entidad.TipoMovimiento;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de ajuste (RF-AJU-001). Una entrada pondera el CPP con el costo indicado; una salida sale al CPP vigente
 * (DP-08).
 */
@Getter
@RequiredArgsConstructor
public enum TipoAjuste {
    /** Aumenta las existencias (incluido el inventario inicial). */
    ENTRADA(TipoMovimiento.AJUSTE_ENTRADA),
    /** Disminuye las existencias (daños, pérdidas, diferencias de conteo). */
    SALIDA(TipoMovimiento.AJUSTE_SALIDA);

    /** Movimiento de kardex que genera el ajuste. */
    private final TipoMovimiento movimiento;
}
