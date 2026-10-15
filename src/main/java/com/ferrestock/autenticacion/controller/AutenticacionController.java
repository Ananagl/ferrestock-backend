/*
 * Nombre:                 AutenticacionController.java
 * Descripción:            Endpoints de autenticación: iniciar sesión, sesión actual, cambio de contraseña e idioma.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.controller;

import com.ferrestock.autenticacion.dto.respuesta.SesionResponse;
import com.ferrestock.autenticacion.dto.solicitud.CambioContrasenaRequest;
import com.ferrestock.autenticacion.dto.solicitud.IdiomaRequest;
import com.ferrestock.autenticacion.dto.solicitud.InicioSesionRequest;
import com.ferrestock.autenticacion.servicio.AutenticacionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de autenticación (04_api_rest.md, sección 4.1). El cierre de sesión ({@code POST /api/v1/auth/logout}) lo
 * atiende directamente el filtro de Spring Security configurado en {@code SeguridadConfig}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AutenticacionController {

    private final AutenticacionServicio servicio;

    /**
     * Inicia sesión con usuario y contraseña.
     *
     * @param solicitud credenciales y "Recordarme"
     * @param peticion  petición HTTP
     * @param respuesta respuesta HTTP
     * @return datos de la sesión
     */
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Inicia sesión (requiere cabecera X-XSRF-TOKEN)")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada; cookies FSSESSION y, si aplica, FSRECUERDAME")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas (mensaje genérico)")
    @ApiResponse(responseCode = "423", description = "Cuenta bloqueada temporalmente")
    public SesionResponse iniciarSesion(@Valid @RequestBody InicioSesionRequest solicitud,
                                        HttpServletRequest peticion, HttpServletResponse respuesta) {
        return servicio.iniciarSesion(solicitud, peticion, respuesta);
    }

    /**
     * @return datos del usuario en sesión
     */
    @GetMapping("/sesion")
    @Operation(summary = "Devuelve la sesión actual")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    public SesionResponse sesion() {
        return servicio.sesionActual();
    }

    /**
     * Cambia la contraseña propia.
     *
     * @param solicitud contraseñas
     * @param peticion  petición HTTP
     * @param respuesta respuesta HTTP
     * @return 204 sin contenido
     */
    @PutMapping("/contrasena")
    @Operation(summary = "Cambia la contraseña propia")
    @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta, confirmación distinta o política")
    public ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambioContrasenaRequest solicitud,
                                                  HttpServletRequest peticion, HttpServletResponse respuesta) {
        servicio.cambiarContrasena(solicitud, peticion, respuesta);
        return ResponseEntity.noContent().build();
    }

    /**
     * Guarda el idioma preferido.
     *
     * @param solicitud idioma
     * @param peticion  petición HTTP
     * @param respuesta respuesta HTTP
     * @return 204 sin contenido
     */
    @PutMapping("/idioma")
    @Operation(summary = "Guarda el idioma preferido (es | en)")
    public ResponseEntity<Void> cambiarIdioma(@Valid @RequestBody IdiomaRequest solicitud,
                                              HttpServletRequest peticion, HttpServletResponse respuesta) {
        servicio.cambiarIdioma(solicitud.idioma(), peticion, respuesta);
        return ResponseEntity.noContent().build();
    }
}
