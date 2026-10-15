/*
 * Nombre:                 AuditoriaResponse.java
 * Descripción:            DTO de salida de un evento de auditoría.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.auditoria.dto.respuesta;

import java.time.Instant;

import com.ferrestock.auditoria.entidad.Auditoria;

/**
 * Evento de auditoría expuesto por la API.
 *
 * @param id            identificador
 * @param usuarioId     usuario responsable (nulo si el login falló con un usuario inexistente)
 * @param nombreUsuario nombre del usuario
 * @param fecha         fecha y hora (UTC)
 * @param accion        código de la acción
 * @param descripcion   descripción legible
 */
public record AuditoriaResponse(Long id, Long usuarioId, String nombreUsuario, Instant fecha, String accion,
                                String descripcion) {

    /**
     * @param entidad evento persistido
     * @return DTO equivalente
     */
    public static AuditoriaResponse desde(Auditoria entidad) {
        return new AuditoriaResponse(entidad.getId(), entidad.getUsuarioId(), entidad.getNombreUsuario(),
            entidad.getFecha(), entidad.getAccion(), entidad.getDescripcion());
    }
}
