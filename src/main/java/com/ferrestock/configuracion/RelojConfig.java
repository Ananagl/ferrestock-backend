/*
 * Nombre:                 RelojConfig.java
 * Descripción:            Reloj del sistema inyectable, para poder controlar el tiempo en las pruebas.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.configuracion;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone un {@link Clock} en UTC. Los servicios lo usan en lugar de {@code Instant.now()}.
 */
@Configuration
public class RelojConfig {

    /**
     * @return reloj del sistema en UTC
     */
    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
