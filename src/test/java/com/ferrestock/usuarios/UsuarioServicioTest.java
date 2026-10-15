/*
 * Nombre:                 UsuarioServicioTest.java
 * Descripción:            Pruebas unitarias con Mockito de las reglas de la gestión de usuarios (RF-AUT-006).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.usuarios.dto.solicitud.UsuarioActualizarRequest;
import com.ferrestock.usuarios.dto.solicitud.UsuarioCrearRequest;
import com.ferrestock.usuarios.entidad.Rol;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import com.ferrestock.usuarios.servicio.FiltroUsuarios;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T15:00:00Z");
    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);

    @Mock private UsuarioRepositorio usuarios;
    @Mock private PersistentTokenRepository tokens;
    @Mock private AuditoriaServicio auditoria;

    private UsuarioServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioServicio(usuarios, codificador, tokens, auditoria, Clock.fixed(AHORA, ZoneOffset.UTC));
        UsuarioAutenticado admin = new UsuarioAutenticado(1L, "admin", "Admin", "ADMIN", null, true, null, false,
            "es", "sello");
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario usuario(long id, String nombre, Rol rol) {
        return Usuario.builder().id(id).nombreUsuario(nombre).nombreCompleto("Nombre " + nombre).rol(rol)
            .passwordHash("hash").debeCambiarPassword(false).build();
    }

    @Test
    @DisplayName("Crear normaliza el nombre a minúsculas, cifra la contraseña temporal y audita")
    void crearNormalizaYCifra() {
        when(usuarios.findByNombreUsuarioIgnoreCase("maria.r")).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });

        var creado = servicio.crear(new UsuarioCrearRequest("  Maria.R ", " María Rincón ", Rol.OPERATIVO,
            "Temporal2026"));

        assertThat(creado.nombreUsuario()).isEqualTo("maria.r");
        assertThat(creado.nombreCompleto()).isEqualTo("María Rincón");
        assertThat(creado.debeCambiarPassword()).isTrue();
        assertThat(creado.activo()).isTrue();
        verify(auditoria).registrar(eq(AccionAuditoria.USUARIO_CREADO), any());
    }

    @Test
    @DisplayName("Crear con un nombre existente (sin importar mayúsculas) es DUPLICADO en el campo nombreUsuario")
    void crearDuplicado() {
        when(usuarios.findByNombreUsuarioIgnoreCase("operador1"))
            .thenReturn(Optional.of(usuario(2L, "operador1", Rol.OPERATIVO)));
        assertThatThrownBy(() -> servicio.crear(new UsuarioCrearRequest("OPERADOR1", "X Y", Rol.OPERATIVO,
            "Temporal2026")))
            .isInstanceOfSatisfying(NegocioException.class, e -> {
                assertThat(e.getCodigo()).isEqualTo(CodigoError.DUPLICADO);
                assertThat(e.getCampo()).isEqualTo("nombreUsuario");
            });
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Un administrador no puede desactivarse a sí mismo")
    void noDesactivarseASiMismo() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN)));
        assertThatThrownBy(() -> servicio.cambiarEstado(1L, false))
            .isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.OPERACION_SOBRE_SI_MISMO));
    }

    @Test
    @DisplayName("Un administrador no puede cambiarse el rol, pero sí su nombre completo")
    void noCambiarPropioRol() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN)));
        assertThatThrownBy(() -> servicio.actualizar(1L, new UsuarioActualizarRequest("Admin", Rol.OPERATIVO)))
            .isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.OPERACION_SOBRE_SI_MISMO));

        when(usuarios.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(servicio.actualizar(1L, new UsuarioActualizarRequest("Ana Admin", Rol.ADMIN)).nombreCompleto())
            .isEqualTo("Ana Admin");
    }

    @Test
    @DisplayName("Desactivar a otro usuario revoca sus tokens de Recordarme y audita")
    void desactivarRevocaTokens() {
        Usuario operador = usuario(5L, "operador1", Rol.OPERATIVO);
        when(usuarios.findById(5L)).thenReturn(Optional.of(operador));
        when(usuarios.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(servicio.cambiarEstado(5L, false).activo()).isFalse();
        verify(tokens).removeUserTokens("operador1");
        verify(auditoria).registrar(eq(AccionAuditoria.USUARIO_DESACTIVADO), any());
    }

    @Test
    @DisplayName("Cambiar el rol de otro usuario revoca sus tokens y registra ROL_CAMBIADO")
    void cambiarRolDeOtro() {
        Usuario operador = usuario(5L, "operador1", Rol.OPERATIVO);
        when(usuarios.findById(5L)).thenReturn(Optional.of(operador));
        when(usuarios.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(servicio.actualizar(5L, new UsuarioActualizarRequest("Nombre operador1", Rol.ADMIN)).rol())
            .isEqualTo("ADMIN");
        verify(tokens).removeUserTokens("operador1");
        verify(auditoria).registrar(eq(AccionAuditoria.ROL_CAMBIADO), any());
        verify(auditoria, never()).registrar(eq(AccionAuditoria.USUARIO_ACTUALIZADO), any());
    }

    @Test
    @DisplayName("Restablecer asigna contraseña temporal, quita el bloqueo y revoca tokens")
    void restablecer() {
        Usuario bloqueado = usuario(5L, "operador1", Rol.OPERATIVO);
        bloqueado.setIntentosFallidos((short) 3);
        bloqueado.setBloqueadoHasta(AHORA.plusSeconds(600));
        when(usuarios.findById(5L)).thenReturn(Optional.of(bloqueado));

        servicio.restablecerContrasena(5L, "NuevaTemporal1");

        assertThat(codificador.matches("NuevaTemporal1", bloqueado.getPasswordHash())).isTrue();
        assertThat(bloqueado.isDebeCambiarPassword()).isTrue();
        assertThat(bloqueado.getBloqueadoHasta()).isNull();
        assertThat(bloqueado.getIntentosFallidos()).isZero();
        verify(tokens).removeUserTokens("operador1");
    }

    @Test
    @DisplayName("Un orden fuera de la lista blanca se rechaza como VALIDACION")
    void ordenInvalido() {
        assertThatThrownBy(() -> servicio.listar(new FiltroUsuarios(null, null, null), 0, 20, "passwordHash,asc"))
            .isInstanceOfSatisfying(NegocioException.class, e -> assertThat(e.getCampo()).isEqualTo("orden"));
    }

    @Test
    @DisplayName("Un id inexistente es NO_ENCONTRADO")
    void noEncontrado() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.obtener(99L))
            .isInstanceOfSatisfying(NegocioException.class,
                e -> assertThat(e.getCodigo()).isEqualTo(CodigoError.NO_ENCONTRADO));
    }
}
