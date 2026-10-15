/*
 * Nombre:                 CambioContrasenaTest.java
 * Descripción:            Pruebas unitarias con Mockito de las reglas del cambio de contraseña (RF-AUT-007).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.autenticacion.dto.solicitud.CambioContrasenaRequest;
import com.ferrestock.autenticacion.seguridad.RecuerdameServicio;
import com.ferrestock.autenticacion.servicio.AutenticacionServicio;
import com.ferrestock.autenticacion.servicio.BloqueoCuentaServicio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.usuarios.entidad.Rol;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class CambioContrasenaTest {

    /** BCrypt de fuerza mínima para que la prueba sea rápida. */
    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);

    @Mock private AuthenticationManager gestor;
    @Mock private SecurityContextRepository repositorioContexto;
    @Mock private RecuerdameServicio recuerdame;
    @Mock private PersistentTokenRepository tokens;
    @Mock private BloqueoCuentaServicio bloqueo;
    @Mock private AuditoriaServicio auditoria;
    @Mock private UsuarioRepositorio usuarios;

    private AutenticacionServicio servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new AutenticacionServicio(gestor, repositorioContexto, recuerdame, tokens, bloqueo, auditoria,
            usuarios, codificador);
        usuario = Usuario.builder().id(3L).nombreUsuario("maria").nombreCompleto("María").rol(Rol.OPERATIVO)
            .passwordHash(codificador.encode("Temporal123")).debeCambiarPassword(true).build();
        UsuarioAutenticado principal = new UsuarioAutenticado(3L, "maria", "María", "OPERATIVO", null, true, null,
            true, "es", UsuarioAutenticado.sellar(usuario.getPasswordHash()));
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        when(usuarios.findById(3L)).thenReturn(Optional.of(usuario));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void cambiar(String actual, String nueva, String confirmacion) {
        servicio.cambiarContrasena(new CambioContrasenaRequest(actual, nueva, confirmacion),
            new MockHttpServletRequest(), new MockHttpServletResponse());
    }

    private static void esperarError(Runnable accion, CodigoError codigo, String campo) {
        assertThatThrownBy(accion::run).isInstanceOf(NegocioException.class)
            .satisfies(e -> {
                assertThat(((NegocioException) e).getCodigo()).isEqualTo(codigo);
                assertThat(((NegocioException) e).getCampo()).isEqualTo(campo);
            });
    }

    @Test
    @DisplayName("Cambio válido: guarda el nuevo hash, quita la marca, revoca Recordarme y audita")
    void cambioValido() {
        cambiar("Temporal123", "Ferreteria2026", "Ferreteria2026");

        assertThat(codificador.matches("Ferreteria2026", usuario.getPasswordHash())).isTrue();
        assertThat(usuario.isDebeCambiarPassword()).isFalse();
        verify(tokens).removeUserTokens("maria");
        verify(repositorioContexto).saveContext(any(), any(), any());
        verify(auditoria).registrar(any(AccionAuditoria.class), any());
        UsuarioAutenticado enSesion =
            (UsuarioAutenticado) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(enSesion.isDebeCambiarPassword()).isFalse();
        assertThat(enSesion.getSelloCredenciales()).isEqualTo(UsuarioAutenticado.sellar(usuario.getPasswordHash()));
    }

    @Test
    @DisplayName("Rechaza una contraseña actual incorrecta (campo actual)")
    void actualIncorrecta() {
        esperarError(() -> cambiar("Otra12345", "Ferreteria2026", "Ferreteria2026"),
            CodigoError.CONTRASENA_ACTUAL_INCORRECTA, "actual");
        verify(tokens, never()).removeUserTokens(any());
    }

    @Test
    @DisplayName("Rechaza una confirmación distinta (campo confirmacion)")
    void confirmacionDistinta() {
        esperarError(() -> cambiar("Temporal123", "Ferreteria2026", "Ferreteria2027"),
            CodigoError.CONTRASENA_NO_COINCIDE, "confirmacion");
    }

    @Test
    @DisplayName("Rechaza repetir la contraseña actual (campo nueva)")
    void repetida() {
        esperarError(() -> cambiar("Temporal123", "Temporal123", "Temporal123"),
            CodigoError.CONTRASENA_REPETIDA, "nueva");
    }

    @Test
    @DisplayName("Rechaza una contraseña que contiene el nombre de usuario (campo nueva)")
    void contieneUsuario() {
        esperarError(() -> cambiar("Temporal123", "Maria2026x", "Maria2026x"),
            CodigoError.CONTRASENA_CONTIENE_USUARIO, "nueva");
    }
}
