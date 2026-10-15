/*
 * Nombre:                 AutenticacionServicio.java
 * Descripción:            Casos de uso de autenticación: iniciar sesión, sesión actual, cambio de contraseña e idioma.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.autenticacion.servicio;

import java.util.Locale;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.autenticacion.dto.respuesta.SesionResponse;
import com.ferrestock.autenticacion.dto.solicitud.CambioContrasenaRequest;
import com.ferrestock.autenticacion.dto.solicitud.InicioSesionRequest;
import com.ferrestock.autenticacion.seguridad.RecuerdameServicio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de autenticación con sesión de servidor y "Recordarme" (ADR-03; 05_seguridad.md).
 */
@Service
@RequiredArgsConstructor
public class AutenticacionServicio {

    private final AuthenticationManager gestorAutenticacion;
    private final SecurityContextRepository repositorioContexto;
    private final RecuerdameServicio recuerdame;
    private final PersistentTokenRepository tokensRecuerdame;
    private final BloqueoCuentaServicio bloqueo;
    private final AuditoriaServicio auditoria;
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;

    /** Cambia el id de sesión al autenticar, para impedir la fijación de sesión. */
    private final SessionAuthenticationStrategy estrategiaSesion = new ChangeSessionIdAuthenticationStrategy();

    /**
     * Inicia sesión (RF-AUT-001, RF-AUT-002, RF-AUT-008). Ante credenciales inválidas o una cuenta desactivada
     * responde siempre con el mismo error genérico, sin revelar qué falló.
     *
     * @param solicitud credenciales y opción "Recordarme"
     * @param peticion  petición HTTP
     * @param respuesta respuesta HTTP (recibe las cookies de sesión)
     * @return datos de la sesión iniciada
     * @throws NegocioException {@code CREDENCIALES_INVALIDAS} o {@code CUENTA_BLOQUEADA}
     */
    public SesionResponse iniciarSesion(InicioSesionRequest solicitud, HttpServletRequest peticion,
                                        HttpServletResponse respuesta) {
        String nombre = solicitud.usuario().trim().toLowerCase(Locale.ROOT);
        Authentication autenticacion;
        try {
            autenticacion = gestorAutenticacion.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(nombre, solicitud.contrasena()));
        } catch (LockedException e) {
            auditoria.registrarIndependiente(null, nombre, AccionAuditoria.LOGIN_FALLIDO,
                "Intento de ingreso con la cuenta bloqueada");
            throw new NegocioException(CodigoError.CUENTA_BLOQUEADA);
        } catch (DisabledException e) {
            auditoria.registrarIndependiente(null, nombre, AccionAuditoria.LOGIN_FALLIDO,
                "Intento de ingreso con una cuenta desactivada");
            throw new NegocioException(CodigoError.CREDENCIALES_INVALIDAS);
        } catch (BadCredentialsException e) {
            BloqueoCuentaServicio.ResultadoFallo fallo = bloqueo.registrarFallo(nombre);
            auditoria.registrarIndependiente(fallo.usuarioId(), nombre, AccionAuditoria.LOGIN_FALLIDO,
                "Usuario o contraseña incorrectos");
            if (fallo.bloqueada()) {
                auditoria.registrarIndependiente(fallo.usuarioId(), nombre, AccionAuditoria.CUENTA_BLOQUEADA,
                    "Cuenta bloqueada temporalmente por intentos fallidos");
            }
            throw new NegocioException(CodigoError.CREDENCIALES_INVALIDAS);
        }

        UsuarioAutenticado usuario = (UsuarioAutenticado) autenticacion.getPrincipal();
        estrategiaSesion.onAuthentication(autenticacion, peticion, respuesta);
        guardarEnSesion(autenticacion, peticion, respuesta);
        if (solicitud.recordarme()) {
            peticion.setAttribute(RecuerdameServicio.ATRIBUTO_SOLICITADO, Boolean.TRUE);
            recuerdame.loginSuccess(peticion, respuesta, autenticacion);
        }
        bloqueo.registrarExito(usuario.getId());
        auditoria.registrarIndependiente(usuario.getId(), usuario.getNombreUsuario(), AccionAuditoria.LOGIN_EXITOSO,
            solicitud.recordarme() ? "Inicio de sesión con Recordarme" : "Inicio de sesión");
        return SesionResponse.desde(usuario);
    }

    /**
     * Devuelve los datos actuales del usuario en sesión, leídos de la base.
     *
     * @return sesión actual
     * @throws NegocioException {@code NO_AUTENTICADO} si el usuario ya no existe
     */
    @Transactional(readOnly = true)
    public SesionResponse sesionActual() {
        return SesionResponse.desde(usuarioEnBase());
    }

    /**
     * Cambia la contraseña propia (RF-AUT-007). Quita la marca de cambio obligatorio y cierra las sesiones
     * persistentes ("Recordarme") de todos los dispositivos; la sesión actual sigue activa.
     *
     * @param solicitud contraseñas actual, nueva y confirmación
     * @param peticion  petición HTTP
     * @param respuesta respuesta HTTP
     * @throws NegocioException si la actual es incorrecta, no coincide la confirmación, se repite la actual o la
     *                          nueva contiene el nombre de usuario
     */
    @Transactional
    public void cambiarContrasena(CambioContrasenaRequest solicitud, HttpServletRequest peticion,
                                  HttpServletResponse respuesta) {
        Usuario usuario = usuarioEnBase();
        if (!codificador.matches(solicitud.actual(), usuario.getPasswordHash())) {
            throw NegocioException.enCampo(CodigoError.CONTRASENA_ACTUAL_INCORRECTA, "actual");
        }
        if (!solicitud.nueva().equals(solicitud.confirmacion())) {
            throw NegocioException.enCampo(CodigoError.CONTRASENA_NO_COINCIDE, "confirmacion");
        }
        if (codificador.matches(solicitud.nueva(), usuario.getPasswordHash())) {
            throw NegocioException.enCampo(CodigoError.CONTRASENA_REPETIDA, "nueva");
        }
        if (solicitud.nueva().toLowerCase(Locale.ROOT).contains(usuario.getNombreUsuario().toLowerCase(Locale.ROOT))) {
            throw NegocioException.enCampo(CodigoError.CONTRASENA_CONTIENE_USUARIO, "nueva");
        }
        usuario.setPasswordHash(codificador.encode(solicitud.nueva()));
        usuario.setDebeCambiarPassword(false);
        usuario.setActualizadoPorId(usuario.getId());
        tokensRecuerdame.removeUserTokens(usuario.getNombreUsuario());
        actualizarPrincipal(usuario, peticion, respuesta);
        auditoria.registrar(AccionAuditoria.CONTRASENA_CAMBIADA, "Cambio de contraseña propia");
    }

    /**
     * Guarda el idioma preferido del usuario.
     *
     * @param idioma    {@code es} o {@code en}
     * @param peticion  petición HTTP
     * @param respuesta respuesta HTTP
     */
    @Transactional
    public void cambiarIdioma(String idioma, HttpServletRequest peticion, HttpServletResponse respuesta) {
        Usuario usuario = usuarioEnBase();
        usuario.setIdioma(idioma);
        actualizarPrincipal(usuario, peticion, respuesta);
    }

    private Usuario usuarioEnBase() {
        Long id = UsuarioActual.obtener().map(UsuarioAutenticado::getId)
            .orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
        return usuarios.findById(id).orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
    }

    /** Reemplaza el principal de la sesión para reflejar cambios sin obligar a ingresar de nuevo. */
    private void actualizarPrincipal(Usuario usuario, HttpServletRequest peticion, HttpServletResponse respuesta) {
        UsuarioAutenticado actual = UsuarioActual.obtener().orElseThrow();
        UsuarioAutenticado nuevo = actual.con(usuario.isDebeCambiarPassword(), usuario.getIdioma(),
            UsuarioAutenticado.sellar(usuario.getPasswordHash()));
        guardarEnSesion(UsernamePasswordAuthenticationToken.authenticated(nuevo, null, nuevo.getAuthorities()),
            peticion, respuesta);
    }

    private void guardarEnSesion(Authentication autenticacion, HttpServletRequest peticion,
                                 HttpServletResponse respuesta) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.saveContext(contexto, peticion, respuesta);
    }
}
