/*
 * Nombre:                 OpenApiConfig.java
 * Descripción:            Metadatos de la documentación OpenAPI/Swagger y esquema de seguridad por cookie de sesión.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-14
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.1.0
 */
package com.ferrestock.configuracion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de springdoc-openapi (RNF-COM-02).
 */
@Configuration
public class OpenApiConfig {

    /** Nombre del esquema de seguridad basado en la cookie de sesión. */
    private static final String ESQUEMA_COOKIE = "cookieAuth";

    /**
     * Describe la API y declara la autenticación por cookie {@code FSSESSION}.
     *
     * @return definición OpenAPI base
     */
    @Bean
    public OpenAPI definicionOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("FerreStock API")
                .version("v1")
                .description("API REST JSON del sistema de inventario FerreStock para ferretería.")
                .license(new License().name("Uso académico")))
            .components(new Components().addSecuritySchemes(ESQUEMA_COOKIE,
                new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("FSSESSION")))
            .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_COOKIE));
    }
}
