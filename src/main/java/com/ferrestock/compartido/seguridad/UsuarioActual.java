/*
 * Nombre:                 UsuarioActual.java
 * Descripción:            Acceso al usuario autenticado de la petición en curso.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compartido.seguridad;

import java.util.Optional;

import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;

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

    /**
     * @return el usuario autenticado
     * @throws NegocioException {@code NO_AUTENTICADO} si la petición es anónima
     */
    public static UsuarioAutenticado exigir() {
        return obtener().orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
    }
}
