/*
 * Nombre:                 SesionResponse.java
 * Descripción:            DTO de salida con los datos de la sesión actual.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.dto.respuesta;

import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.usuarios.entidad.Usuario;

/**
 * Datos del usuario en sesión que necesita el frontend.
 *
 * @param id                  identificador
 * @param nombreUsuario       nombre de usuario
 * @param nombreCompleto      nombre para mostrar
 * @param rol                 ADMIN u OPERATIVO
 * @param idioma              idioma preferido
 * @param debeCambiarPassword si debe cambiar la contraseña antes de continuar
 */
public record SesionResponse(Long id, String nombreUsuario, String nombreCompleto, String rol, String idioma,
                             boolean debeCambiarPassword) {

    /**
     * @param usuario principal de la sesión
     * @return DTO equivalente
     */
    public static SesionResponse desde(UsuarioAutenticado usuario) {
        return new SesionResponse(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombreCompleto(),
            usuario.getRol(), usuario.getIdioma(), usuario.isDebeCambiarPassword());
    }

    /**
     * @param usuario entidad recién leída de la base
     * @return DTO equivalente
     */
    public static SesionResponse desde(Usuario usuario) {
        return new SesionResponse(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombreCompleto(),
            usuario.getRol().name(), usuario.getIdioma(), usuario.isDebeCambiarPassword());
    }
}
