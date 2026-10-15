/*
 * Nombre:                 TerceroServicio.java
 * Descripción:            Casos de uso de clientes y proveedores (RF-TER-001 a 006).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.5.0
 */
package com.ferrestock.terceros.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.respuesta.OrdenSeguro;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.compartido.validacion.ValidadorNit;
import com.ferrestock.terceros.dto.respuesta.TerceroResponse;
import com.ferrestock.terceros.dto.solicitud.TerceroActualizarRequest;
import com.ferrestock.terceros.dto.solicitud.TerceroCrearRequest;
import com.ferrestock.terceros.entidad.Tercero;
import com.ferrestock.terceros.entidad.TipoDocumento;
import com.ferrestock.terceros.entidad.TipoPersona;
import com.ferrestock.terceros.repositorio.TerceroRepositorio;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de terceros. Reglas (RN-10, DP-06):
 * <ul>
 *   <li>Un registro por persona o empresa, con los roles cliente y proveedor combinables (al menos uno).</li>
 *   <li>Tipo + número de documento únicos (RNP-02).</li>
 *   <li>Persona jurídica solo con NIT; el NIT exige un dígito de verificación válido (módulo 11 de la DIAN).</li>
 *   <li>El documento no cambia después del alta. Desactivar es exclusivo del ADMIN y conserva el historial.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TerceroServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("nombre", "numeroDocumento", "creadoEn");

    private final TerceroRepositorio terceros;
    private final AuditoriaServicio auditoria;

    /**
     * @param filtro filtros opcionales
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @param orden  campo,dirección de la lista blanca
     * @return página de terceros
     */
    @Transactional(readOnly = true)
    public PaginaResponse<TerceroResponse> listar(FiltroTerceros filtro, int pagina, int tamano, String orden) {
        Sort ordenamiento = OrdenSeguro.de(orden, CAMPOS_ORDEN, Sort.by("nombre")).and(Sort.by("id"));
        return PaginaResponse.de(terceros.findAll(especificacion(filtro),
            PaginaResponse.solicitud(pagina, tamano, ordenamiento)).map(TerceroResponse::desde));
    }

    /**
     * @param id tercero
     * @return el tercero
     */
    @Transactional(readOnly = true)
    public TerceroResponse obtener(Long id) {
        return TerceroResponse.desde(buscar(id));
    }

    /**
     * Registra un tercero activo (RF-TER-001 a 003).
     *
     * @param solicitud datos del tercero
     * @return tercero creado
     */
    @Transactional
    public TerceroResponse crear(TerceroCrearRequest solicitud) {
        exigirAlgunRol(solicitud.esCliente(), solicitud.esProveedor());
        if (solicitud.tipoPersona() == TipoPersona.JURIDICA && solicitud.tipoDocumento() != TipoDocumento.NIT) {
            throw new NegocioException(CodigoError.PERSONA_DOCUMENTO_INCOMPATIBLE, "tipoDocumento");
        }
        String digito = null;
        if (solicitud.tipoDocumento() == TipoDocumento.NIT) {
            if (!ValidadorNit.esValido(solicitud.numeroDocumento(), solicitud.digitoVerificacion())) {
                throw new NegocioException(CodigoError.DIGITO_VERIFICACION_INVALIDO, "digitoVerificacion");
            }
            digito = solicitud.digitoVerificacion();
        }
        terceros.findByTipoDocumentoAndNumeroDocumento(solicitud.tipoDocumento(), solicitud.numeroDocumento())
            .ifPresent(existente -> {
                throw new NegocioException(CodigoError.DUPLICADO, "numeroDocumento");
            });
        Long actor = actorId();
        Tercero tercero = terceros.saveAndFlush(Tercero.builder()
            .tipoPersona(solicitud.tipoPersona())
            .tipoDocumento(solicitud.tipoDocumento())
            .numeroDocumento(solicitud.numeroDocumento())
            .digitoVerificacion(digito)
            .nombre(solicitud.nombre().trim())
            .telefono(opcional(solicitud.telefono()))
            .correo(opcional(solicitud.correo()))
            .direccion(opcional(solicitud.direccion()))
            .esCliente(solicitud.esCliente())
            .esProveedor(solicitud.esProveedor())
            .creadoPorId(actor)
            .actualizadoPorId(actor)
            .build());
        auditoria.registrar(AccionAuditoria.TERCERO_CREADO, "Alta del tercero " + documento(tercero) + " ("
            + tercero.getNombre() + ") como " + roles(tercero));
        return TerceroResponse.desde(tercero);
    }

    /**
     * Modifica nombre, contacto y roles (RF-TER-002, RF-TER-005). El documento no cambia.
     *
     * @param id        tercero
     * @param solicitud nuevos datos
     * @return tercero actualizado
     */
    @Transactional
    public TerceroResponse actualizar(Long id, TerceroActualizarRequest solicitud) {
        exigirAlgunRol(solicitud.esCliente(), solicitud.esProveedor());
        Tercero tercero = buscar(id);
        tercero.setNombre(solicitud.nombre().trim());
        tercero.setTelefono(opcional(solicitud.telefono()));
        tercero.setCorreo(opcional(solicitud.correo()));
        tercero.setDireccion(opcional(solicitud.direccion()));
        tercero.setEsCliente(solicitud.esCliente());
        tercero.setEsProveedor(solicitud.esProveedor());
        tercero.setActualizadoPorId(actorId());
        Tercero guardado = terceros.saveAndFlush(tercero);
        auditoria.registrar(AccionAuditoria.TERCERO_ACTUALIZADO, "Datos del tercero " + documento(guardado)
            + " actualizados; roles: " + roles(guardado));
        return TerceroResponse.desde(guardado);
    }

    /**
     * Activa o desactiva un tercero (RF-TER-006). Un tercero inactivo no se usa en nuevas compras o ventas.
     *
     * @param id     tercero
     * @param activo nuevo estado
     * @return tercero actualizado
     */
    @Transactional
    public TerceroResponse cambiarEstado(Long id, boolean activo) {
        Tercero tercero = buscar(id);
        if (tercero.isActivo() != activo) {
            tercero.setActivo(activo);
            tercero.setActualizadoPorId(actorId());
            tercero = terceros.saveAndFlush(tercero);
            auditoria.registrar(activo ? AccionAuditoria.TERCERO_ACTIVADO : AccionAuditoria.TERCERO_DESACTIVADO,
                (activo ? "Reactivación" : "Desactivación") + " del tercero " + documento(tercero));
        }
        return TerceroResponse.desde(tercero);
    }

    private Tercero buscar(Long id) {
        return terceros.findById(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
    }

    private static void exigirAlgunRol(boolean esCliente, boolean esProveedor) {
        if (!esCliente && !esProveedor) {
            throw new NegocioException(CodigoError.ROL_TERCERO_REQUERIDO, "esCliente");
        }
    }

    private static String documento(Tercero tercero) {
        return tercero.getTipoDocumento() + " " + tercero.getNumeroDocumento()
            + (tercero.getDigitoVerificacion() == null ? "" : "-" + tercero.getDigitoVerificacion());
    }

    private static String roles(Tercero tercero) {
        return tercero.isEsCliente() && tercero.isEsProveedor() ? "cliente y proveedor"
            : tercero.isEsCliente() ? "cliente" : "proveedor";
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static Long actorId() {
        return UsuarioActual.obtener().map(UsuarioAutenticado::getId)
            .orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
    }

    private static Specification<Tercero> especificacion(FiltroTerceros filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.rol() == FiltroTerceros.RolTercero.CLIENTE) {
                condiciones.add(cb.isTrue(raiz.get("esCliente")));
            } else if (filtro.rol() == FiltroTerceros.RolTercero.PROVEEDOR) {
                condiciones.add(cb.isTrue(raiz.get("esProveedor")));
            }
            if (filtro.tipoPersona() != null) {
                condiciones.add(cb.equal(raiz.get("tipoPersona"), filtro.tipoPersona()));
            }
            if (filtro.activo() != null) {
                condiciones.add(cb.equal(raiz.get("activo"), filtro.activo()));
            }
            if (filtro.texto() != null && !filtro.texto().isBlank()) {
                String patron = "%" + filtro.texto().trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                condiciones.add(cb.or(
                    cb.like(cb.lower(raiz.get("nombre")), patron, '\\'),
                    cb.like(raiz.get("numeroDocumento"), patron, '\\')));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
