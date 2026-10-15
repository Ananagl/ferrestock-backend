/*
 * Nombre:                 FerreStockApplication.java
 * Descripción:            Punto de entrada de la aplicación Spring Boot de FerreStock.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Aplicación principal del backend de FerreStock.
 *
 * <p>Se excluye la configuración automática de usuarios en memoria de Spring Boot: los usuarios
 * se leerán de la tabla {@code usuario} (módulo {@code autenticacion}, Fase 4).</p>
 */
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class FerreStockApplication {

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(FerreStockApplication.class, args);
    }
}
