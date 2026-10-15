/*
 * Nombre:                 CsrfController.java
 * Descripción:            Endpoint público que entrega la cookie XSRF-TOKEN a la SPA antes de enviar formularios.
 * Fecha de creación:      2026-10-14
 * Fecha de actualización: 2026-10-14
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.1.0
 */
package com.ferrestock.autenticacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicializa la protección CSRF del cliente web (RNF-SEG-03).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
public class CsrfController {

    /**
     * Fuerza la generación del token CSRF; Spring Security lo envía en la cookie {@code XSRF-TOKEN}.
     *
     * @param token token CSRF de la petición actual
     * @return respuesta vacía 204
     */
    @GetMapping("/csrf")
    @Operation(summary = "Inicializa la cookie XSRF-TOKEN")
    @SecurityRequirements
    public ResponseEntity<Void> inicializar(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
