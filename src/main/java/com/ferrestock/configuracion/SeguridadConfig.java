/*
 * Nombre:                 SeguridadConfig.java
 * Descripción:            Configuración de Spring Security: sesión de servidor, Recordarme persistente, CSRF con cookie, cierre de sesión, errores JSON y cambio de contraseña obligatorio.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.configuracion;

import javax.sql.DataSource;

import com.ferrestock.autenticacion.seguridad.CambioContrasenaPendienteFiltro;
import com.ferrestock.autenticacion.seguridad.RecuerdameServicio;
import com.ferrestock.autenticacion.seguridad.RegistroCierreSesion;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.EscritorProblemas;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Seguridad del backend. Diseño de referencia: {@code docs/fase-2-diseno/05_seguridad.md}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    /** Fuerza de BCrypt definida en RNF-SEG-01. */
    private static final int FUERZA_BCRYPT = 12;

    /** Nombre de la cookie de sesión (también configurado en application.yml). */
    public static final String COOKIE_SESION = "FSSESSION";

    /**
     * Cadena de filtros de seguridad.
     *
     * @param http           constructor de la configuración HTTP
     * @param propiedades    parámetros de seguridad
     * @param recuerdame     servicio de sesión persistente
     * @param contexto       repositorio del contexto de seguridad en la sesión HTTP
     * @param registroCierre manejador que audita el cierre de sesión
     * @param escritor       generador de respuestas de error JSON
     * @return la cadena de filtros configurada
     * @throws Exception si la configuración no es válida
     */
    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http, PropiedadesSeguridad propiedades,
                                             RecuerdameServicio recuerdame, SecurityContextRepository contexto,
                                             RegistroCierreSesion registroCierre, EscritorProblemas escritor)
            throws Exception {
        http
            // CSRF con cookie legible por la SPA (XSRF-TOKEN -> cabecera X-XSRF-TOKEN), incluido el login
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .securityContext(sc -> sc.securityContextRepository(contexto))
            .authorizeHttpRequests(rutas -> {
                rutas.requestMatchers("/actuator/health", "/api/v1/auth/csrf", "/api/v1/auth/login").permitAll();
                if (propiedades.swaggerPublico()) {
                    rutas.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                } else {
                    rutas.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN");
                }
                rutas.anyRequest().authenticated();
            })
            .rememberMe(rm -> rm.rememberMeServices(recuerdame).key(propiedades.recuerdameClave()))
            .logout(salida -> salida
                .logoutUrl("/api/v1/auth/logout")
                .addLogoutHandler(registroCierre)
                .deleteCookies(COOKIE_SESION, RecuerdameServicio.COOKIE)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            // Sin formularios ni Basic: errores en JSON y el frontend redirige al login
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((pet, resp, ex) -> escritor.escribir(pet, resp, CodigoError.NO_AUTENTICADO))
                .accessDeniedHandler((pet, resp, ex) -> escritor.escribir(pet, resp, CodigoError.ACCESO_DENEGADO)))
            .addFilterAfter(new CambioContrasenaPendienteFiltro(escritor), AuthorizationFilter.class)
            .headers(cabeceras -> cabeceras
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }

    /**
     * Gestor de autenticación con usuario y contraseña contra la tabla {@code usuario}.
     *
     * @param usuarios    servicio de carga de usuarios
     * @param codificador codificador BCrypt
     * @return gestor de autenticación
     */
    @Bean
    public AuthenticationManager gestorAutenticacion(UserDetailsService usuarios, PasswordEncoder codificador) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarios);
        proveedor.setPasswordEncoder(codificador);
        return new ProviderManager(proveedor);
    }

    /**
     * @return repositorio que guarda el contexto de seguridad en la sesión HTTP
     */
    @Bean
    public SecurityContextRepository repositorioContexto() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * @param fuenteDatos conexión a PostgreSQL
     * @return repositorio de tokens de "Recordarme" en la tabla {@code persistent_logins}
     */
    @Bean
    public PersistentTokenRepository repositorioTokensRecuerdame(DataSource fuenteDatos) {
        JdbcTokenRepositoryImpl repositorio = new JdbcTokenRepositoryImpl();
        repositorio.setDataSource(fuenteDatos);
        return repositorio;
    }

    /**
     * @param propiedades parámetros de seguridad
     * @param usuarios    servicio de carga de usuarios
     * @param tokens      repositorio de tokens
     * @return servicio de sesión persistente
     */
    @Bean
    public RecuerdameServicio recuerdameServicio(PropiedadesSeguridad propiedades, UserDetailsService usuarios,
                                                 PersistentTokenRepository tokens) {
        return new RecuerdameServicio(propiedades.recuerdameClave(), usuarios, tokens, propiedades.recuerdameDias(),
            propiedades.cookiesSeguras());
    }

    /**
     * Aplica {@code SameSite=Lax} a todas las cookies (sesión, Recordarme y XSRF-TOKEN).
     *
     * @return proveedor del atributo SameSite
     */
    @Bean
    public CookieSameSiteSupplier cookiesSameSiteLax() {
        return CookieSameSiteSupplier.ofLax();
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
