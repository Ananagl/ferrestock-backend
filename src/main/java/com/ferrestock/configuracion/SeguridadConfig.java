/*
 * Nombre:                 SeguridadConfig.java
 * Descripción:            Configuración base de Spring Security: rutas públicas, CSRF con cookie para la SPA,
 *                         respuesta 401 sin formularios y codificador BCrypt.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-14
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.1.0
 */
package com.ferrestock.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Configuración de seguridad de la base técnica (Fase 3).
 *
 * <p>Diseño de referencia: {@code docs/fase-2-diseno/05_seguridad.md}. En la Fase 4 se agregan el
 * inicio de sesión, "Recordarme" y el bloqueo por intentos fallidos.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    /** Fuerza de BCrypt definida en RNF-SEG-01. */
    private static final int FUERZA_BCRYPT = 12;

    /**
     * Cadena de filtros de seguridad.
     *
     * @param http           constructor de la configuración HTTP
     * @param swaggerPublico si la documentación OpenAPI es visible sin sesión (solo en el perfil dev)
     * @return la cadena de filtros configurada
     * @throws Exception si la configuración no es válida
     */
    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http,
            @Value("${ferrestock.seguridad.swagger-publico:false}") boolean swaggerPublico) throws Exception {
        http
            // CSRF con cookie legible por la SPA (XSRF-TOKEN -> cabecera X-XSRF-TOKEN)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(rutas -> {
                rutas.requestMatchers("/actuator/health", "/api/v1/auth/csrf").permitAll();
                if (swaggerPublico) {
                    rutas.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                }
                rutas.anyRequest().authenticated();
            })
            // Sin formularios ni Basic: la API responde 401 y el frontend redirige al login
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(cabeceras -> cabeceras
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    /**
     * Codificador de contraseñas BCrypt compatible con los hashes creados por {@code db/crear_bd.sh}.
     *
     * @return codificador BCrypt de fuerza 12
     */
    @Bean
    public PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder(FUERZA_BCRYPT);
    }
}
