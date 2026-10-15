/*
 * Nombre:                 PostgresDePrueba.java
 * Descripción:            Contenedor PostgreSQL efímero para pruebas de integración, inicializado con los mismos
 *                         scripts SQL del directorio db/ que se usan en la instalación real.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-14
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.1.0
 */
package com.ferrestock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Configuración de pruebas que levanta PostgreSQL 16 real (no H2), según ADR-16.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresDePrueba {

    /**
     * Contenedor PostgreSQL cuyos datos de conexión Spring Boot toma automáticamente.
     *
     * @return contenedor con el esquema y los datos iniciales aplicados
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ferrestock")
            .withCopyFileToContainer(MountableFile.forHostPath("db/01_esquema.sql"),
                "/docker-entrypoint-initdb.d/01_esquema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("db/02_datos_iniciales.sql"),
                "/docker-entrypoint-initdb.d/02_datos_iniciales.sql");
    }
}
