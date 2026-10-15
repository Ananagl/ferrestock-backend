/*
 * Nombre:                 ValidadorContrasenaSeguraTest.java
 * Descripción:            Pruebas unitarias y de límite de la política de contraseñas (RNF-SEG-01).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.compartido;

import static org.assertj.core.api.Assertions.assertThat;

import com.ferrestock.compartido.validacion.ValidadorContrasenaSegura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * Casos de límite: 7, 8, 72 y 73 caracteres; sin letras; sin números.
 */
class ValidadorContrasenaSeguraTest {

    private final ValidadorContrasenaSegura validador = new ValidadorContrasenaSegura();

    @ParameterizedTest(name = "[{index}] longitud {0} con letras y números -> {1}")
    @CsvSource({"7,false", "8,true", "72,true", "73,false"})
    @DisplayName("Respeta los límites de longitud 8 a 72")
    void limitesDeLongitud(int longitud, boolean esperado) {
        String valor = "a1" + "b".repeat(longitud - 2);
        assertThat(validador.isValid(valor, null)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({"abcdefgh,false", "12345678,false", "abcd1234,true", "ñandú2026,true"})
    @DisplayName("Exige al menos una letra y un número")
    void exigeLetraYNumero(String valor, boolean esperado) {
        assertThat(validador.isValid(valor, null)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Un valor nulo lo controla @NotBlank, no este validador")
    void nuloEsValido(String valor) {
        assertThat(validador.isValid(valor, null)).isTrue();
    }
}
