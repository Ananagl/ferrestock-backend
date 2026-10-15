/*
 * Nombre:                 MensajesI18nTest.java
 * Descripción:            Prueba unitaria que garantiza que los archivos de mensajes en español e inglés
 *                         tengan exactamente las mismas claves (RNF-USA-01).
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-14
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.1.0
 */
package com.ferrestock.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica la paridad de claves entre idiomas.
 */
class MensajesI18nTest {

    private static Properties cargar(String recurso) throws IOException {
        Properties propiedades = new Properties();
        try (Reader lector = new InputStreamReader(
                MensajesI18nTest.class.getResourceAsStream(recurso), StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        }
        return propiedades;
    }

    @Test
    @DisplayName("Español e inglés tienen las mismas claves y ningún mensaje vacío")
    void mismasClavesEnAmbosIdiomas() throws IOException {
        Properties es = cargar("/i18n/mensajes.properties");
        Properties en = cargar("/i18n/mensajes_en.properties");

        assertThat(es.stringPropertyNames()).isNotEmpty().isEqualTo(en.stringPropertyNames());
        assertThat(es.values()).allSatisfy(v -> assertThat(v.toString()).isNotBlank());
        assertThat(en.values()).allSatisfy(v -> assertThat(v.toString()).isNotBlank());
    }
}
