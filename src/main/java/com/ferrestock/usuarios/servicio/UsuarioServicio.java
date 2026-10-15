/*
 * Nombre:                 UsuarioServicio.java
 * Descripción:            Casos de uso de la gestión de usuarios por el Administrador (RF-AUT-006).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.6.0
 */
package com.ferrestock.usuarios.servicio;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.respuesta.OrdenSeguro;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.usuarios.dto.respuesta.UsuarioResponse;
import com.ferrestock.usuarios.dto.solicitud.UsuarioActualizarRequest;
import com.ferrestock.usuarios.dto.solicitud.UsuarioCrearRequest;
import com.ferrestock.usuarios.entidad.Usuario;
import com.ferrestock.usuarios.repositorio.UsuarioRepositorio;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de cuentas. Reglas:
 * <ul>
 *   <li>El nombre de usuario se guarda en minúsculas y es único sin distinguir mayúsculas.</li>
 *   <li>Toda contraseña asignada por el administrador es temporal ({@code debeCambiarPassword}).</li>
 *   <li>Un administrador no puede desactivarse, cambiarse el rol ni restablecerse la contraseña a sí mismo; así
 *       siempre queda al menos un administrador activo.</li>
 *   <li>Desactivar, cambiar el rol o restablecer la contraseña revoca los tokens de "Recordarme"; el filtro
 *       {@code EstadoCuentaFiltro} cierra además las sesiones abiertas.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UsuarioServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("nombreUsuario", "nombreCompleto", "rol", "creadoEn");

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final PersistentTokenRepository tokensRecuerdame;
    private final AuditoriaServicio auditoria;
    private final Clock reloj;

    /**
     * Lista usuarios con filtros y paginación.
     *
     * @param filtro filtros opcionales
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @param orden  campo y dirección, por ejemplo {@code nombreUsuario,asc}
     * @return página de usuarios
     */
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(FiltroUsuarios filtro, int pagina, int tamano, String orden) {
        Sort ordenamiento = OrdenSeguro.de(orden, CAMPOS_ORDEN, Sort.by("nombreUsuario"));
        return PaginaResponse.de(usuarios.findAll(especificacion(filtro),
                PaginaResponse.solicitud(pagina, tamano, ordenamiento.and(Sort.by("id"))))
            .map(u -> UsuarioResponse.desde(u, reloj.instant())));
    }

    /**
     * @param id identificador
     * @return el usuario
     * @throws NegocioException {@code NO_ENCONTRADO}
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id), reloj.instant());
    }

    /**
     * Crea un usuario activo con contraseña temporal.
     *
     * @param solicitud datos del usuario
     * @return usuario creado
     * @throws NegocioException {@code DUPLICADO} si el nombre de usuario ya existe
     */
    @Transactional
    public UsuarioResponse crear(UsuarioCrearRequest solicitud) {
        String nombre = solicitud.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByNombreUsuarioIgnoreCase(nombre).isPresent()) {
            throw NegocioException.enCampo(CodigoError.DUPLICADO, "nombreUsuario");
        }
        Long actor = actor().getId();
        Usuario usuario = usuarios.save(Usuario.builder()
            .nombreUsuario(nombre)
            .nombreCompleto(solicitud.nombreCompleto().trim())
            .rol(solicitud.rol())
            .passwordHash(codificador.encode(solicitud.contrasenaTemporal()))
            .debeCambiarPassword(true)
            .creadoPorId(actor)
            .actualizadoPorId(actor)
            .build());
        auditoria.registrar(AccionAuditoria.USUARIO_CREADO,
            "Alta del usuario " + nombre + " con rol " + solicitud.rol());
        return UsuarioResponse.desde(usuario, reloj.instant());
    }

    /**
     * Modifica el nombre completo y el rol.
     *
     * @param id        identificador
     * @param solicitud nuevos datos
     * @return usuario actualizado
     * @throws NegocioException {@code OPERACION_SOBRE_SI_MISMO} si el administrador intenta cambiar su propio rol
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest solicitud) {
        Usuario usuario = buscar(id);
        boolean cambiaRol = usuario.getRol() != solicitud.rol();
        if (cambiaRol) {
            noSobreSiMismo(usuario);
        }
        String nombreCompleto = solicitud.nombreCompleto().trim();
        boolean cambiaNombre = !Objects.equals(usuario.getNombreCompleto(), nombreCompleto);
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setActualizadoPorId(actor().getId());
        if (cambiaRol) {
            String anterior = usuario.getRol().name();
            usuario.setRol(solicitud.rol());
            tokensRecuerdame.removeUserTokens(usuario.getNombreUsuario());
            auditoria.registrar(AccionAuditoria.ROL_CAMBIADO,
                "Rol de " + usuario.getNombreUsuario() + " cambiado de " + anterior + " a " + solicitud.rol());
        }
        if (cambiaNombre) {
            auditoria.registrar(AccionAuditoria.USUARIO_ACTUALIZADO,
                "Nombre completo de " + usuario.getNombreUsuario() + " actualizado");
        }
        return UsuarioResponse.desde(usuarios.saveAndFlush(usuario), reloj.instant());
    }

    /**
     * Activa o desactiva un usuario (eliminación lógica).
     *
     * @param id     identificador
     * @param activo nuevo estado
     * @return usuario actualizado
     * @throws NegocioException {@code OPERACION_SOBRE_SI_MISMO} si el administrador intenta desactivarse
     */
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario usuario = buscar(id);
        if (usuario.isActivo() == activo) {
            return UsuarioResponse.desde(usuario, reloj.instant());
        }
        if (!activo) {
            noSobreSiMismo(usuario);
            tokensRecuerdame.removeUserTokens(usuario.getNombreUsuario());
        }
        usuario.setActivo(activo);
        usuario.setActualizadoPorId(actor().getId());
        auditoria.registrar(activo ? AccionAuditoria.USUARIO_ACTIVADO : AccionAuditoria.USUARIO_DESACTIVADO,
            (activo ? "Reactivación del usuario " : "Desactivación del usuario ") + usuario.getNombreUsuario());
        return UsuarioResponse.desde(usuarios.saveAndFlush(usuario), reloj.instant());
    }

    /**
     * Asigna una contraseña temporal, quita el bloqueo por intentos y cierra las sesiones del usuario.
     *
     * @param id                 identificador
     * @param contrasenaTemporal nueva contraseña temporal
     * @throws NegocioException {@code OPERACION_SOBRE_SI_MISMO} si es la propia cuenta (debe usar el cambio de
     *                          contraseña)
     */
    @Transactional
    public void restablecerContrasena(Long id, String contrasenaTemporal) {
        Usuario usuario = buscar(id);
        noSobreSiMismo(usuario);
        usuario.setPasswordHash(codificador.encode(contrasenaTemporal));
        usuario.setDebeCambiarPassword(true);
        usuario.setIntentosFallidos((short) 0);
        usuario.setBloqueadoHasta(null);
        usuario.setActualizadoPorId(actor().getId());
        tokensRecuerdame.removeUserTokens(usuario.getNombreUsuario());
        auditoria.registrar(AccionAuditoria.CONTRASENA_RESTABLECIDA,
            "Contraseña temporal asignada a " + usuario.getNombreUsuario());
    }

    /**
     * Nombres de usuario por id, para mostrar quién hizo cada operación (por ejemplo, en el kardex).
     *
     * @param ids ids de usuario
     * @return mapa id → nombre de usuario
     */
    @Transactional(readOnly = true)
    public Map<Long, String> nombresPorId(Collection<Long> ids) {
        return usuarios.findAllById(ids).stream()
            .collect(java.util.stream.Collectors.toMap(Usuario::getId, Usuario::getNombreUsuario));
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
    }

    private static UsuarioAutenticado actor() {
        return UsuarioActual.obtener().orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
    }

    private static void noSobreSiMismo(Usuario usuario) {
        if (usuario.getId().equals(actor().getId())) {
            throw new NegocioException(CodigoError.OPERACION_SOBRE_SI_MISMO);
        }
    }

    private static Specification<Usuario> especificacion(FiltroUsuarios filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.rol() != null) {
                condiciones.add(cb.equal(raiz.get("rol"), filtro.rol()));
            }
            if (filtro.activo() != null) {
                condiciones.add(cb.equal(raiz.get("activo"), filtro.activo()));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = "%" + filtro.texto().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                condiciones.add(cb.or(
                    cb.like(cb.lower(raiz.get("nombreUsuario")), patron, '\\'),
                    cb.like(cb.lower(raiz.get("nombreCompleto")), patron, '\\')));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
