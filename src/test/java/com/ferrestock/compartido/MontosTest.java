/*
 * Nombre:                 MontosTest.java
 * Descripción:            Pruebas unitarias del costo promedio ponderado y del redondeo con los valores de CA-COS-01, 02 y 03.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.compartido;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.ferrestock.compartido.numeros.Montos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MontosTest {

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("CA-COS-01: 10 a 1.000 + 30 a 1.400 → CPP 1.300,0000")
    void caCos01() {
        assertThat(Montos.costoPromedioPonderado(d("10"), d("1000"), d("30"), d("1400"))).isEqualTo("1300.0000");
    }

    @Test
    @DisplayName("CA-COS-02: con existencias en cero, el CPP es el costo de la entrada")
    void caCos02() {
        assertThat(Montos.costoPromedioPonderado(d("0.000"), d("0"), d("5"), d("2000"))).isEqualTo("2000.0000");
    }

    @Test
    @DisplayName("CA-COS-03: 10 a 1.200 + devolución de 2 al costo histórico 1.000 → 1.166,6667")
    void caCos03() {
        assertThat(Montos.costoPromedioPonderado(d("10"), d("1200"), d("2"), d("1000"))).isEqualTo("1166.6667");
    }

    @Test
    @DisplayName("Cantidades fraccionarias: 2,5 m a 3.000 + 1,25 m a 3.600 → 3.200,0000")
    void fracciones() {
        assertThat(Montos.costoPromedioPonderado(d("2.5"), d("3000"), d("1.25"), d("3600"))).isEqualTo("3200.0000");
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"12500.005,12500.01", "12500.004,12500.00", "0.005,0.01"})
    @DisplayName("El dinero redondea a 2 decimales con HALF_UP")
    void redondeoDinero(String valor, String esperado) {
        assertThat(Montos.dinero(d(valor))).isEqualTo(esperado);
    }

    @Test
    @DisplayName("El subtotal es cantidad × precio redondeado a 2 decimales")
    void subtotal() {
        assertThat(Montos.subtotal(d("2.500"), d("2500.50"))).isEqualTo("6251.25");
        assertThat(Montos.subtotal(d("3"), d("33.333"))).isEqualTo("100.00");
    }

    @ParameterizedTest(name = "{0} entera={1}")
    @CsvSource({"3,true", "3.000,true", "3.500,false", "0.001,false"})
    @DisplayName("Detecta cantidades enteras sin importar los ceros decimales")
    void esEntera(String valor, boolean entera) {
        assertThat(Montos.esEntera(d(valor))).isEqualTo(entera);
    }
}
