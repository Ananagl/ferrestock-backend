/*
 * Nombre:                 UsuarioResponse.java
 * Descripción:            DTO de salida de un usuario (nunca incluye el hash de la contraseña).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios.dto.respuesta;

import java.time.Instant;

import com.ferrestock.usuarios.entidad.Usuario;

/**
 * Usuario expuesto por la API.
 *
 * @param id                  identificador
 * @param nombreUsuario       nombre de usuario
 * @param nombreCompleto      nombre para mostrar
 * @param rol                 ADMIN u OPERATIVO
 * @param activo              si puede ingresar
 * @param bloqueado           si está bloqueado temporalmente por intentos fallidos
 * @param debeCambiarPassword si tiene una contraseña temporal pendiente de cambio
 * @param creadoEn            fecha de alta
 * @param actualizadoEn       fecha de la última modificación
 */
public record UsuarioResponse(Long id, String nombreUsuario, String nombreCompleto, String rol, boolean activo,
                              boolean bloqueado, boolean debeCambiarPassword, Instant creadoEn, Instant actualizadoEn) {

    /**
     * @param usuario entidad
     * @param ahora   instante actual, para calcular el bloqueo
     * @return DTO equivalente
     */
    public static UsuarioResponse desde(Usuario usuario, Instant ahora) {
        boolean bloqueado = usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora);
        return new UsuarioResponse(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombreCompleto(),
            usuario.getRol().name(), usuario.isActivo(), bloqueado, usuario.isDebeCambiarPassword(),
            usuario.getCreadoEn(), usuario.getActualizadoEn());
    }
}
