/*
 * Nombre:                 ValidadorNitTest.java
 * Descripción:            Pruebas unitarias y de límite del dígito de verificación del NIT con NIT públicos conocidos.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.compartido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ferrestock.compartido.validacion.ValidadorNit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorNitTest {

    @ParameterizedTest(name = "NIT {0}-{1}")
    @CsvSource({"890903938,8", "899999068,1", "860034313,7", "800197268,4"})
    @DisplayName("Calcula el DV correcto de NIT públicos (Bancolombia, Ecopetrol, Davivienda, DIAN)")
    void nitsPublicos(String numero, int digito) {
        assertThat(ValidadorNit.calcularDigito(numero)).isEqualTo(digito);
        assertThat(ValidadorNit.esValido(numero, String.valueOf(digito))).isTrue();
    }

    @Test
    @DisplayName("CA-TER-04: rechaza un DV que no corresponde")
    void digitoIncorrecto() {
        assertThat(ValidadorNit.esValido("890903938", "7")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12", "x", " "})
    @DisplayName("Un DV ausente o que no sea un único dígito es inválido")
    void digitoMalFormado(String digito) {
        assertThat(ValidadorNit.esValido("890903938", digito)).isFalse();
    }

    @Test
    @DisplayName("Los residuos 0 y 1 son el propio DV; el resto es 11 menos el residuo")
    void residuosBajos() {
        // 1 × 3 = 3 → residuo 3 → DV 8; 4 × 3 = 12 → residuo 1 → DV 1; 0 → residuo 0 → DV 0
        assertThat(ValidadorNit.calcularDigito("1")).isEqualTo(8);
        assertThat(ValidadorNit.calcularDigito("4")).isEqualTo(1);
        assertThat(ValidadorNit.calcularDigito("0")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12a45", "1234567890123456"})
    @DisplayName("Rechaza números con letras, vacíos o de más de 15 dígitos")
    void numeroInvalido(String numero) {
        assertThatThrownBy(() -> ValidadorNit.calcularDigito(numero)).isInstanceOf(IllegalArgumentException.class);
    }
}
