/*
 * Nombre:                 BloqueoCuentaServicio.java
 * Descripción:            Cuenta los intentos fallidos y bloquea temporalmente la cuenta (RF-AUT-008).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.autenticacion.servicio;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

import com.ferrestock.configuracion.PropiedadesSeguridad;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Control de intentos fallidos. Cada operación usa su propia transacción para que el conteo se guarde aunque el
 * inicio de sesión termine en error.
 */
@Service
@RequiredArgsConstructor
public class BloqueoCuentaServicio {

    private final UsuarioRepositorio usuarios;
    private final PropiedadesSeguridad propiedades;
    private final Clock reloj;

    /**
     * Resultado de registrar un intento fallido.
     *
     * @param usuarioId id del usuario, o {@code null} si el nombre no existe
     * @param bloqueada {@code true} si este intento provocó el bloqueo
     */
    public record ResultadoFallo(Long usuarioId, boolean bloqueada) {
    }

    /**
     * Suma un intento fallido. Al llegar al máximo, bloquea la cuenta por los minutos configurados y reinicia el
     * contador, para que al vencer el bloqueo haya de nuevo el número completo de intentos.
     *
     * @param nombreUsuario nombre escrito en el login
     * @return resultado del registro
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResultadoFallo registrarFallo(String nombreUsuario) {
        return usuarios.findByNombreUsuarioIgnoreCase(nombreUsuario.toLowerCase(Locale.ROOT))
            .map(this::sumarIntento)
            .orElse(new ResultadoFallo(null, false));
    }

    /**
     * Reinicia el contador y quita cualquier bloqueo tras un inicio de sesión correcto.
     *
     * @param usuarioId id del usuario
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(Long usuarioId) {
        usuarios.findById(usuarioId).ifPresent(usuario -> {
            usuario.setIntentosFallidos((short) 0);
            usuario.setBloqueadoHasta(null);
        });
    }

    private ResultadoFallo sumarIntento(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= propiedades.intentosMaximos()) {
            usuario.setIntentosFallidos((short) 0);
            usuario.setBloqueadoHasta(reloj.instant().plus(Duration.ofMinutes(propiedades.minutosBloqueo())));
            return new ResultadoFallo(usuario.getId(), true);
        }
        usuario.setIntentosFallidos((short) intentos);
        return new ResultadoFallo(usuario.getId(), false);
    }
}
