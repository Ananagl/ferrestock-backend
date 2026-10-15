/*
 * Nombre:                 FiltroUsuarios.java
 * Descripción:            Criterios de búsqueda del listado de usuarios.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios.servicio;

import com.ferrestock.usuarios.entidad.Rol;

/**
 * Filtros opcionales del listado de usuarios.
 *
 * @param texto  texto contenido en el nombre de usuario o el nombre completo
 * @param rol    rol exacto
 * @param activo estado
 */
public record FiltroUsuarios(String texto, Rol rol, Boolean activo) {
}
