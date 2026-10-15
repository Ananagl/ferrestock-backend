/*
 * Nombre:                 DetallesUsuarioServicio.java
 * Descripción:            Carga los usuarios de la base de datos para Spring Security.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.seguridad;

import java.util.Locale;

import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link UserDetailsService} sobre la tabla {@code usuario}. La usan el inicio de sesión y el
 * inicio automático por "Recordarme".
 */
@Service
@RequiredArgsConstructor
public class DetallesUsuarioServicio implements UserDetailsService {

    private final UsuarioRepositorio usuarios;

    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado loadUserByUsername(String nombreUsuario) {
        return usuarios.findByNombreUsuarioIgnoreCase(nombreUsuario.trim().toLowerCase(Locale.ROOT))
            .map(DetallesUsuarioServicio::aPrincipal)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /**
     * Convierte la entidad en el principal de la sesión.
     *
     * @param usuario entidad
     * @return principal con el hash de contraseña (se borra tras autenticar)
     */
    public static UsuarioAutenticado aPrincipal(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombreCompleto(),
            usuario.getRol().name(), usuario.getPasswordHash(), usuario.isActivo(), usuario.getBloqueadoHasta(),
            usuario.isDebeCambiarPassword(), usuario.getIdioma());
    }
}
