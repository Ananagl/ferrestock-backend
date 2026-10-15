/*
 * Nombre:                 EstadoCuentaFiltro.java
 * Descripción:            Filtro que en cada petición a la API comprueba en la base que la cuenta en sesión siga activa,
 *                         con el mismo rol y la misma contraseña, y que obliga a cambiar la contraseña temporal.
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.3.0
 */
package com.ferrestock.autenticacion.seguridad;

import java.io.IOException;
import java.util.Optional;

import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.EscritorProblemas;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Control del estado de la cuenta en cada petición autenticada a {@code /api/**} (ADR-19):
 *
 * <ul>
 *   <li>Si el usuario fue desactivado, cambió de rol o el administrador le restableció la contraseña, la sesión se
 *       invalida y la petición responde 401 {@code NO_AUTENTICADO}: debe ingresar de nuevo.</li>
 *   <li>Si tiene {@code debeCambiarPassword}, solo puede usar {@code /api/v1/auth/**}; lo demás responde 403
 *       {@code CAMBIO_CONTRASENA_REQUERIDO}.</li>
 * </ul>
 *
 * <p>La consulta es por clave primaria, de costo despreciable para el volumen del proyecto, y cubre también las
 * sesiones restauradas con "Recordarme". No es un {@code @Component}: solo vive en la cadena de Spring Security.</p>
 */
@RequiredArgsConstructor
public class EstadoCuentaFiltro extends OncePerRequestFilter {

    private static final String RUTAS_AUTENTICACION = "/api/v1/auth/";

    private final UsuarioRepositorio usuarios;
    private final EscritorProblemas escritor;

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String ruta = solicitud.getRequestURI().substring(solicitud.getContextPath().length());
        Optional<UsuarioAutenticado> enSesion = UsuarioActual.obtener();
        if (enSesion.isEmpty() || !ruta.startsWith("/api/")) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        UsuarioAutenticado principal = enSesion.get();
        Optional<Usuario> enBase = usuarios.findById(principal.getId());
        if (enBase.isEmpty() || !sigueVigente(principal, enBase.get())) {
            HttpSession sesion = solicitud.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            SecurityContextHolder.clearContext();
            escritor.escribir(solicitud, respuesta, CodigoError.NO_AUTENTICADO);
            return;
        }
        if (enBase.get().isDebeCambiarPassword() && !ruta.startsWith(RUTAS_AUTENTICACION)) {
            escritor.escribir(solicitud, respuesta, CodigoError.CAMBIO_CONTRASENA_REQUERIDO);
            return;
        }
        cadena.doFilter(solicitud, respuesta);
    }

    /**
     * @return {@code true} si la cuenta sigue activa, con el mismo rol y la misma contraseña que al iniciar sesión
     */
    private static boolean sigueVigente(UsuarioAutenticado principal, Usuario usuario) {
        return usuario.isActivo()
            && usuario.getRol().name().equals(principal.getRol())
            && UsuarioAutenticado.sellar(usuario.getPasswordHash()).equals(principal.getSelloCredenciales());
    }
}
