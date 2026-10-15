/*
 * Nombre:                 UsuarioActual.java
 * Descripción:            Acceso al usuario autenticado de la petición en curso.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.compartido.seguridad;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilidad para obtener el usuario autenticado desde cualquier servicio.
 */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    /**
     * @return el usuario autenticado, o vacío si la petición es anónima
     */
    public static Optional<UsuarioAutenticado> obtener() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }
}
