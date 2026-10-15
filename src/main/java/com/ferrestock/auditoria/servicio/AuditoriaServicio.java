/*
 * Nombre:                 AuditoriaServicio.java
 * Descripción:            Registro y consulta de eventos de auditoría (RF-AUD-001, RF-AUD-002).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.2.0
 */
package com.ferrestock.auditoria.servicio;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ferrestock.auditoria.dto.respuesta.AuditoriaResponse;
import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.entidad.Auditoria;
import com.ferrestock.auditoria.repositorio.AuditoriaRepositorio;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de auditoría compartido por todos los módulos.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaServicio {

    /** Zona horaria del negocio para interpretar los filtros por fecha. */
    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

    private static final int MAX_DESCRIPCION = 500;
    private static final int MAX_NOMBRE = 50;

    private final AuditoriaRepositorio repositorio;
    private final Clock reloj;

    /**
     * Registra un evento a nombre del usuario autenticado, dentro de la transacción de la operación: si la
     * operación falla, el evento tampoco queda.
     *
     * @param accion      acción realizada
     * @param descripcion descripción legible
     */
    @Transactional
    public void registrar(AccionAuditoria accion, String descripcion) {
        UsuarioAutenticado usuario = UsuarioActual.obtener().orElse(null);
        guardar(usuario == null ? null : usuario.getId(),
            usuario == null ? "anonimo" : usuario.getNombreUsuario(), accion, descripcion);
    }

    /**
     * Registra un evento en una transacción propia, para que quede aunque la operación que lo origina falle
     * (por ejemplo, un inicio de sesión rechazado).
     *
     * @param usuarioId     usuario relacionado, o {@code null}
     * @param nombreUsuario nombre escrito o del usuario
     * @param accion        acción
     * @param descripcion   descripción legible
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIndependiente(Long usuarioId, String nombreUsuario, AccionAuditoria accion,
                                       String descripcion) {
        guardar(usuarioId, nombreUsuario, accion, descripcion);
    }

    /**
     * Consulta paginada con filtros, del evento más reciente al más antiguo.
     *
     * @param filtro filtros opcionales
     * @param pagina número de página
     * @param tamano tamaño de página (máximo 100)
     * @return página de eventos
     */
    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> consultar(FiltroAuditoria filtro, int pagina, int tamano) {
        var solicitud = PaginaResponse.solicitud(pagina, tamano,
            Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")));
        return PaginaResponse.de(repositorio.findAll(especificacion(filtro), solicitud).map(AuditoriaResponse::desde));
    }

    private void guardar(Long usuarioId, String nombreUsuario, AccionAuditoria accion, String descripcion) {
        repositorio.save(Auditoria.builder()
            .usuarioId(usuarioId)
            .nombreUsuario(recortar(nombreUsuario == null || nombreUsuario.isBlank() ? "anonimo" : nombreUsuario,
                MAX_NOMBRE))
            .fecha(reloj.instant())
            .accion(accion.name())
            .descripcion(recortar(descripcion, MAX_DESCRIPCION))
            .build());
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    /** Construye los criterios con parámetros (sin concatenar SQL). */
    private static Specification<Auditoria> especificacion(FiltroAuditoria filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.usuarioId() != null) {
                condiciones.add(cb.equal(raiz.get("usuarioId"), filtro.usuarioId()));
            }
            if (filtro.accion() != null) {
                condiciones.add(cb.equal(raiz.get("accion"), filtro.accion().name()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fecha"),
                    filtro.desde().atStartOfDay(ZONA_NEGOCIO).toInstant()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThan(raiz.get("fecha"),
                    filtro.hasta().plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant()));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = "%" + filtro.texto().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                condiciones.add(cb.or(
                    cb.like(cb.lower(raiz.get("descripcion")), patron, '\\'),
                    cb.like(cb.lower(raiz.get("nombreUsuario")), patron, '\\')));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
