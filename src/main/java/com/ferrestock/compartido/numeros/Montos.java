/*
 * Nombre:                 Montos.java
 * Descripción:            Aritmética exacta de cantidades, costos y dinero con las escalas y el redondeo del diseño (ADR-10).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.compartido.numeros;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Reglas numéricas únicas del sistema (03_inventario_y_costos.md, sección 4): cantidades con 3 decimales, costos
 * unitarios y CPP con 4, dinero con 2, siempre con {@code HALF_UP}. Toda la aritmética de inventario pasa por aquí.
 */
public final class Montos {

    /** Decimales de las cantidades. */
    public static final int ESCALA_CANTIDAD = 3;
    /** Decimales de costos unitarios y del costo promedio ponderado. */
    public static final int ESCALA_COSTO = 4;
    /** Decimales del dinero en COP. */
    public static final int ESCALA_DINERO = 2;

    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private Montos() {
    }

    /** @param valor cantidad @return cantidad con 3 decimales */
    public static BigDecimal cantidad(BigDecimal valor) {
        return valor.setScale(ESCALA_CANTIDAD, REDONDEO);
    }

    /** @param valor costo @return costo con 4 decimales */
    public static BigDecimal costo(BigDecimal valor) {
        return valor.setScale(ESCALA_COSTO, REDONDEO);
    }

    /** @param valor monto @return monto con 2 decimales */
    public static BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(ESCALA_DINERO, REDONDEO);
    }

    /**
     * Costo promedio ponderado tras una entrada (RF-COS-001): {@code C' = (Q·C + q·c) / (Q + q)}; si {@code Q = 0},
     * {@code C' = c}. Se calcula con DECIMAL64 y se redondea a 4 decimales al final.
     *
     * @param existencias existencias antes de la entrada (Q)
     * @param costoActual costo promedio antes de la entrada (C)
     * @param cantidad    cantidad que entra (q), mayor que cero
     * @param costoEntrada costo unitario de la entrada (c)
     * @return nuevo costo promedio con 4 decimales
     */
    public static BigDecimal costoPromedioPonderado(BigDecimal existencias, BigDecimal costoActual, BigDecimal cantidad,
                                                    BigDecimal costoEntrada) {
        if (existencias.signum() == 0) {
            return costo(costoEntrada);
        }
        BigDecimal valorAnterior = existencias.multiply(costoActual, MathContext.DECIMAL64);
        BigDecimal valorEntrada = cantidad.multiply(costoEntrada, MathContext.DECIMAL64);
        BigDecimal total = existencias.add(cantidad);
        return costo(valorAnterior.add(valorEntrada, MathContext.DECIMAL64).divide(total, MathContext.DECIMAL64));
    }

    /**
     * @param cantidad cantidad
     * @param precio   precio o costo unitario
     * @return subtotal redondeado a 2 decimales
     */
    public static BigDecimal subtotal(BigDecimal cantidad, BigDecimal precio) {
        return dinero(cantidad.multiply(precio, MathContext.DECIMAL64));
    }

    /**
     * @param cantidad cantidad a revisar
     * @return {@code true} si no tiene parte decimal (por ejemplo 3.000)
     */
    public static boolean esEntera(BigDecimal cantidad) {
        return cantidad.stripTrailingZeros().scale() <= 0;
    }
}
