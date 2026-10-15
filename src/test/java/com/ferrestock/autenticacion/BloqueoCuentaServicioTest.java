/*
 * Nombre:                 BloqueoCuentaServicioTest.java
 * Descripción:            Pruebas unitarias con Mockito del bloqueo por intentos fallidos (RF-AUT-008).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.ferrestock.autenticacion.servicio.BloqueoCuentaServicio;
import com.ferrestock.configuracion.PropiedadesSeguridad;
import com.ferrestock.usuarios.entidad.Rol;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BloqueoCuentaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T15:00:00Z");

    @Mock
    private UsuarioRepositorio usuarios;

    private BloqueoCuentaServicio servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        PropiedadesSeguridad propiedades = new PropiedadesSeguridad(false, 5, 15, "clave", 7, false);
        servicio = new BloqueoCuentaServicio(usuarios, propiedades, Clock.fixed(AHORA, ZoneOffset.UTC));
        usuario = Usuario.builder().id(7L).nombreUsuario("operador1").rol(Rol.OPERATIVO).build();
    }

    @Test
    @DisplayName("Los primeros 4 fallos solo suman intentos")
    void cuatroFallosNoBloquean() {
        when(usuarios.findByNombreUsuarioIgnoreCase("operador1")).thenReturn(Optional.of(usuario));
        for (int i = 1; i <= 4; i++) {
            BloqueoCuentaServicio.ResultadoFallo resultado = servicio.registrarFallo("operador1");
            assertThat(resultado.bloqueada()).isFalse();
            assertThat(usuario.getIntentosFallidos()).isEqualTo((short) i);
        }
        assertThat(usuario.getBloqueadoHasta()).isNull();
    }

    @Test
    @DisplayName("El quinto fallo bloquea 15 minutos y reinicia el contador")
    void quintoFalloBloquea() {
        usuario.setIntentosFallidos((short) 4);
        when(usuarios.findByNombreUsuarioIgnoreCase("operador1")).thenReturn(Optional.of(usuario));

        BloqueoCuentaServicio.ResultadoFallo resultado = servicio.registrarFallo("OPERADOR1");

        assertThat(resultado).isEqualTo(new BloqueoCuentaServicio.ResultadoFallo(7L, true));
        assertThat(usuario.getBloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    @DisplayName("Un usuario inexistente no provoca errores ni bloqueos")
    void usuarioInexistente() {
        when(usuarios.findByNombreUsuarioIgnoreCase("nadie")).thenReturn(Optional.empty());
        assertThat(servicio.registrarFallo("nadie")).isEqualTo(new BloqueoCuentaServicio.ResultadoFallo(null, false));
    }

    @Test
    @DisplayName("Un ingreso correcto reinicia intentos y quita el bloqueo")
    void exitoReinicia() {
        usuario.setIntentosFallidos((short) 3);
        usuario.setBloqueadoHasta(AHORA.minusSeconds(1));
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));

        servicio.registrarExito(7L);

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
    }
}
