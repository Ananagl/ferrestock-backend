/*
 * Nombre:                 CambioContrasenaPendienteFiltro.java
 * Descripción:            Filtro que obliga a cambiar la contraseña temporal antes de usar el resto de la API.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.seguridad;

import java.io.IOException;

import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.EscritorProblemas;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Si el usuario autenticado tiene {@code debeCambiarPassword}, solo puede usar {@code /api/v1/auth/**}; cualquier
 * otra ruta de la API responde 403 {@code CAMBIO_CONTRASENA_REQUERIDO} (05_seguridad.md, sección 2).
 *
 * <p>No es un {@code @Component}: se registra solo dentro de la cadena de Spring Security.</p>
 */
@RequiredArgsConstructor
public class CambioContrasenaPendienteFiltro extends OncePerRequestFilter {

    private static final String RUTAS_PERMITIDAS = "/api/v1/auth/";

    private final EscritorProblemas escritor;

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        boolean pendiente = UsuarioActual.obtener().map(u -> u.isDebeCambiarPassword()).orElse(false);
        String ruta = solicitud.getRequestURI().substring(solicitud.getContextPath().length());
        if (pendiente && ruta.startsWith("/api/") && !ruta.startsWith(RUTAS_PERMITIDAS)) {
            escritor.escribir(solicitud, respuesta, CodigoError.CAMBIO_CONTRASENA_REQUERIDO);
            return;
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
