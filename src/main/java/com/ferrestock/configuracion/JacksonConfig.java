/*
 * Nombre:                 JacksonConfig.java
 * Descripción:            Serializa los BigDecimal como cadena decimal sin notación científica (dinero, costos y cantidades), según 04_api_rest.md.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.4.0
 */
package com.ferrestock.configuracion;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los montos viajan como texto ({@code "35000.00"}) para que JavaScript no pierda precisión al convertirlos a
 * número de coma flotante. En la entrada, Jackson acepta tanto texto como número.
 */
@Configuration
public class JacksonConfig {

    /**
     * @return personalización del ObjectMapper de Spring
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer decimalesComoTexto() {
        return constructor -> constructor.serializerByType(BigDecimal.class, new DecimalComoTexto());
    }

    /** Escribe {@code BigDecimal.toPlainString()} para evitar valores como {@code 1E+3}. */
    static final class DecimalComoTexto extends StdSerializer<BigDecimal> {

        DecimalComoTexto() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal valor, JsonGenerator generador, SerializerProvider proveedor)
                throws IOException {
            generador.writeString(valor.toPlainString());
        }
    }
}
