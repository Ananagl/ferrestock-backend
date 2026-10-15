/*
 * Nombre:                 RegistroCierreSesion.java
 * Descripción:            Registra en auditoría cada cierre de sesión.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.seguridad;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Manejador de cierre de sesión que deja el evento {@code LOGOUT} en la auditoría (RF-AUT-003).
 */
@Component
@RequiredArgsConstructor
public class RegistroCierreSesion implements LogoutHandler {

    private final AuditoriaServicio auditoria;

    @Override
    public void logout(HttpServletRequest solicitud, HttpServletResponse respuesta, Authentication autenticacion) {
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            auditoria.registrarIndependiente(usuario.getId(), usuario.getNombreUsuario(), AccionAuditoria.LOGOUT,
                "Cierre de sesión");
        }
    }
}
