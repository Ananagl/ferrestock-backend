/*
 * Nombre:                 AjusteServicio.java
 * Descripción:            Registro e historial de ajustes de inventario, exclusivos del Administrador (RF-AJU, RN-08).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.7.0
 */
package com.ferrestock.ajustes.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ferrestock.ajustes.dto.respuesta.AjusteResponse;
import com.ferrestock.ajustes.dto.solicitud.AjusteCrearRequest;
import com.ferrestock.ajustes.entidad.Ajuste;
import com.ferrestock.ajustes.entidad.TipoAjuste;
import com.ferrestock.ajustes.repositorio.AjusteRepositorio;
import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.catalogo.entidad.EstadoProducto;
import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.catalogo.servicio.ProductoServicio;
import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.numeros.Montos;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.comprobantes.servicio.ConsecutivoServicio;
import com.ferrestock.inventario.entidad.MovimientoInventario;
import com.ferrestock.inventario.servicio.MovimientoInventarioServicio;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ajustes de inventario (CU-09). Un ajuste es un documento con consecutivo propio que genera un único movimiento
 * en el kardex. El inventario inicial se carga como un ajuste de entrada (03_inventario_y_costos.md, sección 2).
 */
@Service
@RequiredArgsConstructor
public class AjusteServicio {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");

    private final AjusteRepositorio ajustes;
    private final ProductoServicio productos;
    private final MovimientoInventarioServicio movimientos;
    private final ConsecutivoServicio consecutivos;
    private final UsuarioServicio usuarios;
    private final AuditoriaServicio auditoria;
    private final Clock reloj;

    /**
     * Registra un ajuste en una sola transacción: bloquea el producto, valida, asigna el consecutivo, guarda el
     * documento, mueve el inventario y audita. Si algo falla, no queda ningún efecto (RNF-FIA-01).
     *
     * @param solicitud datos del ajuste
     * @return ajuste registrado, con las existencias y el CPP resultantes
     * @throws NegocioException {@code PRODUCTO_INACTIVO}, {@code CANTIDAD_INVALIDA},
     *                          {@code CANTIDAD_FRACCIONARIA_NO_PERMITIDA}, {@code COSTO_UNITARIO_REQUERIDO} o
     *                          {@code EXISTENCIAS_INSUFICIENTES}
     */
    @Transactional
    public AjusteResponse registrar(AjusteCrearRequest solicitud) {
        UsuarioAutenticado actor = UsuarioActual.obtener()
            .orElseThrow(() -> new NegocioException(CodigoError.NO_AUTENTICADO));
        // El controlador ya exige ADMIN; esta comprobación protege el servicio si se reutiliza desde otro lugar
        if (!actor.esAdmin()) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        Producto producto = productos.bloquearParaMovimiento(List.of(solicitud.productoId())).get(solicitud.productoId());
        if (producto.getEstado() == EstadoProducto.INACTIVO) {
            throw NegocioException.enCampo(CodigoError.PRODUCTO_INACTIVO, "productoId");
        }
        BigDecimal cantidad = movimientos.validarCantidad(producto, solicitud.cantidad(), "cantidad");
        boolean entrada = solicitud.tipo() == TipoAjuste.ENTRADA;
        if (entrada && solicitud.costoUnitario() == null) {
            throw NegocioException.enCampo(CodigoError.COSTO_UNITARIO_REQUERIDO, "costoUnitario");
        }
        if (!entrada && producto.getExistencias().compareTo(cantidad) < 0) {
            throw NegocioException.enCampo(CodigoError.EXISTENCIAS_INSUFICIENTES, "cantidad", producto.getCodigo(),
                producto.getExistencias().stripTrailingZeros().toPlainString());
        }

        Instant ahora = reloj.instant();
        Ajuste ajuste = ajustes.saveAndFlush(Ajuste.builder()
            .consecutivo(consecutivos.siguiente(TipoDocumentoComercial.AJUSTE))
            .productoId(producto.getId())
            .tipo(solicitud.tipo())
            .cantidad(cantidad)
            .costoUnitario(entrada ? Montos.costo(solicitud.costoUnitario()) : producto.getCostoPromedio())
            .motivo(solicitud.motivo().trim())
            .fecha(ahora)
            .creadoPorId(actor.getId())
            .creadoEn(ahora)
            .build());
        MovimientoInventario movimiento = entrada
            ? movimientos.registrarEntrada(producto, cantidad, ajuste.getCostoUnitario(), solicitud.tipo().getMovimiento(),
                true, TipoDocumentoComercial.AJUSTE, ajuste.getId())
            : movimientos.registrarSalida(producto, cantidad, solicitud.tipo().getMovimiento(),
                TipoDocumentoComercial.AJUSTE, ajuste.getId());

        auditoria.registrar(AccionAuditoria.AJUSTE_REGISTRADO, "Ajuste " + ajuste.getConsecutivo() + ": "
            + ajuste.getTipo() + " de " + cantidad.stripTrailingZeros().toPlainString() + " en " + producto.getCodigo()
            + ". Motivo: " + ajuste.getMotivo());
        return respuesta(ajuste, producto, actor.getNombreUsuario(), movimiento.getSaldoResultante(),
            movimiento.getCostoPromedioResultante());
    }

    /**
     * Historial de ajustes, del más reciente al más antiguo (RF-AJU-004).
     *
     * @param filtro producto, fechas y tipo
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @return página de ajustes con el producto y el responsable
     */
    @Transactional(readOnly = true)
    public PaginaResponse<AjusteResponse> historial(FiltroAjustes filtro, int pagina, int tamano) {
        Page<Ajuste> resultado = ajustes.findAll(especificacion(filtro),
            PaginaResponse.solicitud(pagina, tamano, Sort.by(Sort.Order.desc("id"))));
        Map<Long, Producto> porId = productos.porIds(resultado.map(Ajuste::getProductoId).toSet());
        Map<Long, String> nombres = usuarios.nombresPorId(resultado.map(Ajuste::getCreadoPorId).toSet());
        return PaginaResponse.de(resultado.map(a -> respuesta(a, porId.get(a.getProductoId()),
            nombres.get(a.getCreadoPorId()), null, null)));
    }

    private static AjusteResponse respuesta(Ajuste ajuste, Producto producto, String usuario, BigDecimal existencias,
                                            BigDecimal costoPromedio) {
        return new AjusteResponse(ajuste.getId(), ajuste.getConsecutivo(), ajuste.getFecha(), ajuste.getProductoId(),
            producto == null ? null : producto.getCodigo(), producto == null ? null : producto.getNombre(),
            ajuste.getTipo().name(), ajuste.getCantidad(), ajuste.getCostoUnitario(), ajuste.getMotivo(), usuario,
            existencias, costoPromedio);
    }

    private static Specification<Ajuste> especificacion(FiltroAjustes filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.productoId() != null) {
                condiciones.add(cb.equal(raiz.get("productoId"), filtro.productoId()));
            }
            if (filtro.tipo() != null) {
                condiciones.add(cb.equal(raiz.get("tipo"), filtro.tipo()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fecha"), filtro.desde().atStartOfDay(ZONA_NEGOCIO).toInstant()));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThan(raiz.get("fecha"), filtro.hasta().plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
