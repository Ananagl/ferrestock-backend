/*
 * Nombre:                 UsuarioAutenticado.java
 * Descripción:            Usuario en sesión (principal de Spring Security), compartido por todos los módulos para conocer quién opera.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.compartido.seguridad;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Datos del usuario autenticado que se guardan en la sesión. Se ubica en {@code compartido} porque todos los
 * servicios lo usan para la auditoría y la autorización, y así se evitan dependencias circulares entre módulos.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UsuarioAutenticado implements UserDetails, CredentialsContainer, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EqualsAndHashCode.Include
    private final Long id;
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final String rol;
    private String passwordHash;
    private final boolean activo;
    private final Instant bloqueadoHasta;
    private final boolean debeCambiarPassword;
    private final String idioma;

    /**
     * Huella de la contraseña vigente al iniciar sesión (SHA-256 del hash BCrypt, 16 caracteres). Si el
     * administrador restablece la contraseña, la huella deja de coincidir y la sesión se cierra (ADR-19).
     */
    private final String selloCredenciales;

    /**
     * Calcula la huella de un hash de contraseña.
     *
     * @param passwordHash hash BCrypt guardado en la base
     * @return 16 caracteres hexadecimales
     */
    public static String sellar(String passwordHash) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * Copia con otros valores de contraseña obligatoria, idioma y huella (tras cambiarlos sin cerrar la sesión).
     *
     * @param debeCambiar nuevo valor de la marca de cambio obligatorio
     * @param nuevoIdioma nuevo idioma preferido
     * @param nuevoSello  huella de la contraseña vigente
     * @return copia actualizada sin credenciales
     */
    public UsuarioAutenticado con(boolean debeCambiar, String nuevoIdioma, String nuevoSello) {
        return new UsuarioAutenticado(id, nombreUsuario, nombreCompleto, rol, null, activo, bloqueadoHasta,
            debeCambiar, nuevoIdioma, nuevoSello);
    }

    /** @return {@code true} si el rol es ADMIN */
    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return nombreUsuario;
    }

    @Override
    public boolean isAccountNonLocked() {
        return bloqueadoHasta == null || bloqueadoHasta.isBefore(Instant.now());
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
