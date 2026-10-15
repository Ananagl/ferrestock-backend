/*
 * Nombre:                 UsuarioRepositorio.java
 * Descripción:            Acceso a datos de usuarios.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.usuarios.repositorio;

import java.util.Optional;

import com.ferrestock.usuarios.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data de {@link Usuario}.
 */
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por nombre sin distinguir mayúsculas (coincide con el índice único sobre lower()).
     *
     * @param nombreUsuario nombre de usuario
     * @return el usuario, si existe
     */
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
}
