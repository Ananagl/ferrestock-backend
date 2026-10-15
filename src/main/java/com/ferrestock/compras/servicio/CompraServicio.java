/*
 * Nombre:                 CompraServicio.java
 * Descripción:            Registro, consulta y anulación de compras a proveedores (RF-COM).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.8.0
 */
package com.ferrestock.compras.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
import com.ferrestock.catalogo.entidad.EstadoProducto;
import com.ferrestock.catalogo.entidad.Producto;
import com.ferrestock.catalogo.servicio.ProductoServicio;
import com.ferrestock.compartido.documentos.EstadoDocumento;
import com.ferrestock.compartido.documentos.LineasDocumento;
import com.ferrestock.compartido.documentos.TipoDocumentoComercial;
import com.ferrestock.compartido.error.CodigoError;
import com.ferrestock.compartido.error.NegocioException;
import com.ferrestock.compartido.numeros.Montos;
import com.ferrestock.compartido.respuesta.PaginaResponse;
import com.ferrestock.compartido.seguridad.UsuarioActual;
import com.ferrestock.compartido.seguridad.UsuarioAutenticado;
import com.ferrestock.comprobantes.servicio.ConsecutivoServicio;
import com.ferrestock.compras.dto.respuesta.CompraResponse;
import com.ferrestock.compras.dto.solicitud.CompraCrearRequest;
import com.ferrestock.compras.entidad.Compra;
import com.ferrestock.compras.entidad.CompraLinea;
import com.ferrestock.compras.repositorio.CompraRepositorio;
import com.ferrestock.inventario.entidad.MovimientoInventario;
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.repositorio.MovimientoInventarioRepositorio;
import com.ferrestock.inventario.servicio.MovimientoInventarioServicio;
import com.ferrestock.terceros.entidad.Tercero;
import com.ferrestock.terceros.servicio.FiltroTerceros;
import com.ferrestock.terceros.servicio.TerceroServicio;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compras (CU-04). Una compra se recibe completa en la misma transacción: si una línea falla, no queda nada
 * (RF-COM-002, RF-COM-003). La anulación sigue 03_inventario_y_costos.md, sección 5.2.
 */
@Service
@RequiredArgsConstructor
public class CompraServicio {

    private final CompraRepositorio compras;
    private final ProductoServicio productos;
    private final TerceroServicio terceros;
    private final MovimientoInventarioServicio movimientos;
    private final MovimientoInventarioRepositorio kardex;
    private final ConsecutivoServicio consecutivos;
    private final UsuarioServicio usuarios;
    private final AuditoriaServicio auditoria;
    private final Clock reloj;

    /**
     * Registra y recibe una compra: suma existencias, pondera el CPP y genera un movimiento por línea.
     *
     * @param solicitud proveedor y líneas
     * @return compra registrada con sus líneas
     */
    @Transactional
    public CompraResponse registrar(CompraCrearRequest solicitud) {
        UsuarioAutenticado actor = UsuarioActual.exigir();
        Tercero proveedor = terceros.exigirParaDocumento(solicitud.proveedorId(), FiltroTerceros.RolTercero.PROVEEDOR,
            "proveedorId");
        List<CompraCrearRequest.Linea> lineas = solicitud.lineas();
        LineasDocumento.exigirSinRepetidos(lineas.stream().map(CompraCrearRequest.Linea::productoId).toList());
        Map<Long, Producto> bloqueados = productos.bloquearParaMovimiento(
            lineas.stream().map(CompraCrearRequest.Linea::productoId).toList());

        Instant ahora = reloj.instant();
        Compra compra = Compra.builder()
            .proveedorId(proveedor.getId())
            .fecha(ahora)
            .observacion(solicitud.observacion() == null || solicitud.observacion().isBlank() ? null : solicitud.observacion().trim())
            .creadoPorId(actor.getId())
            .creadoEn(ahora)
            .build();
        BigDecimal total = Montos.dinero(BigDecimal.ZERO);
        for (int i = 0; i < lineas.size(); i++) {
            CompraCrearRequest.Linea linea = lineas.get(i);
            Producto producto = bloqueados.get(linea.productoId());
            if (producto.getEstado() == EstadoProducto.INACTIVO) {
                throw NegocioException.enCampo(CodigoError.PRODUCTO_INACTIVO, LineasDocumento.campo(i, "productoId"));
            }
            BigDecimal cantidad = movimientos.validarCantidad(producto, linea.cantidad(), LineasDocumento.campo(i, "cantidad"));
            BigDecimal costo = Montos.costo(linea.costoUnitario());
            BigDecimal subtotal = Montos.subtotal(cantidad, costo);
            total = total.add(subtotal);
            compra.agregar(CompraLinea.builder().productoId(producto.getId()).cantidad(cantidad).costoUnitario(costo)
                .subtotal(subtotal).build());
        }
        compra.setTotal(total);
        compra.setConsecutivo(consecutivos.siguiente(TipoDocumentoComercial.COMPRA));
        Compra guardada = compras.saveAndFlush(compra);
        for (CompraLinea linea : guardada.getLineas()) {
            movimientos.registrarEntrada(bloqueados.get(linea.getProductoId()), linea.getCantidad(), linea.getCostoUnitario(),
                TipoMovimiento.COMPRA, true, TipoDocumentoComercial.COMPRA, guardada.getId());
        }
        auditoria.registrar(AccionAuditoria.COMPRA_REGISTRADA, "Compra " + guardada.getConsecutivo() + " al proveedor "
            + proveedor.getNombre() + ": " + guardada.getLineas().size() + " línea(s) por " + total.toPlainString());
        return respuesta(guardada, true);
    }

    /**
     * @param filtro proveedor, fechas y estado
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @return compras de la más reciente a la más antigua, sin líneas
     */
    @Transactional(readOnly = true)
    public PaginaResponse<CompraResponse> listar(FiltroCompras filtro, int pagina, int tamano) {
        Page<Compra> resultado = compras.findAll(especificacion(filtro),
            PaginaResponse.solicitud(pagina, tamano, Sort.by(Sort.Order.desc("id"))));
        Map<Long, String> proveedores = terceros.nombresPorId(resultado.map(Compra::getProveedorId).toSet());
        Map<Long, String> nombres = usuarios.nombresPorId(idsUsuarios(resultado.getContent()));
        return PaginaResponse.de(resultado.map(c -> respuesta(c, proveedores, nombres, null)));
    }

    /**
     * @param id compra
     * @return la compra con sus líneas
     */
    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        return respuesta(compras.conLineas(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO)), true);
    }

    /**
     * Anula una compra (solo ADMIN): por cada línea, una salida que retira lo que entró y restaura el CPP anterior.
     * Se rechaza si las existencias no alcanzan o si hubo entradas posteriores que cambiaron el CPP.
     *
     * @param id     compra
     * @param motivo motivo de la anulación
     * @return compra anulada
     */
    @Transactional
    public CompraResponse anular(Long id, String motivo) {
        UsuarioAutenticado actor = UsuarioActual.exigir();
        if (!actor.esAdmin()) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        Compra compra = compras.bloquear(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
        if (compra.getEstado() == EstadoDocumento.ANULADA) {
            throw new NegocioException(CodigoError.DOCUMENTO_ANULADO);
        }
        Map<Long, Producto> bloqueados = productos.bloquearParaMovimiento(
            compra.getLineas().stream().map(CompraLinea::getProductoId).toList());
        // Primero se valida todo; así el error informa el primer producto que impide anular y no queda nada a medias
        List<MovimientoInventario> originales = new ArrayList<>();
        for (CompraLinea linea : compra.getLineas()) {
            Producto producto = bloqueados.get(linea.getProductoId());
            MovimientoInventario original = kardex.findFirstByDocumentoTipoAndDocumentoIdAndProductoIdOrderByIdAsc(
                TipoDocumentoComercial.COMPRA, compra.getId(), producto.getId())
                .orElseThrow(() -> new IllegalStateException("Compra sin movimiento en el kardex: " + compra.getConsecutivo()));
            if (producto.getExistencias().compareTo(linea.getCantidad()) < 0) {
                throw new NegocioException(CodigoError.EXISTENCIAS_INSUFICIENTES_PARA_ANULAR, producto.getCodigo(),
                    producto.getExistencias().stripTrailingZeros().toPlainString());
            }
            if (kardex.hayEntradasPonderadasDespues(producto.getId(), original.getId())) {
                throw new NegocioException(CodigoError.COMPRA_CON_MOVIMIENTOS_POSTERIORES, producto.getCodigo());
            }
            originales.add(original);
        }
        for (int i = 0; i < compra.getLineas().size(); i++) {
            CompraLinea linea = compra.getLineas().get(i);
            movimientos.revertirEntrada(bloqueados.get(linea.getProductoId()), linea.getCantidad(), linea.getCostoUnitario(),
                originales.get(i).getCostoPromedioAnterior(), TipoMovimiento.ANULACION_COMPRA, TipoDocumentoComercial.COMPRA,
                compra.getId());
        }
        compra.setEstado(EstadoDocumento.ANULADA);
        compra.setMotivoAnulacion(motivo.trim());
        compra.setAnuladaPorId(actor.getId());
        compra.setAnuladaEn(reloj.instant());
        Compra guardada = compras.saveAndFlush(compra);
        auditoria.registrar(AccionAuditoria.COMPRA_ANULADA, "Compra " + compra.getConsecutivo() + " anulada. Motivo: "
            + compra.getMotivoAnulacion());
        return respuesta(guardada, true);
    }

    private CompraResponse respuesta(Compra compra, boolean conLineas) {
        Map<Long, String> proveedores = terceros.nombresPorId(List.of(compra.getProveedorId()));
        Map<Long, String> nombres = usuarios.nombresPorId(idsUsuarios(List.of(compra)));
        List<CompraResponse.Linea> lineas = null;
        if (conLineas) {
            Map<Long, Producto> porId = productos.porIds(compra.getLineas().stream().map(CompraLinea::getProductoId).toList());
            lineas = compra.getLineas().stream().map(l -> {
                Producto p = porId.get(l.getProductoId());
                return new CompraResponse.Linea(l.getId(), l.getProductoId(), p.getCodigo(), p.getNombre(), l.getCantidad(),
                    l.getCostoUnitario(), l.getSubtotal());
            }).toList();
        }
        return respuesta(compra, proveedores, nombres, lineas);
    }

    private static CompraResponse respuesta(Compra c, Map<Long, String> proveedores, Map<Long, String> nombres,
                                            List<CompraResponse.Linea> lineas) {
        return new CompraResponse(c.getId(), c.getConsecutivo(), c.getFecha(), c.getProveedorId(),
            proveedores.get(c.getProveedorId()), c.getObservacion(), c.getTotal(), c.getEstado().name(),
            c.getMotivoAnulacion(), c.getAnuladaPorId() == null ? null : nombres.get(c.getAnuladaPorId()), c.getAnuladaEn(),
            nombres.get(c.getCreadoPorId()), lineas);
    }

    private static Set<Long> idsUsuarios(List<Compra> lista) {
        Set<Long> ids = new HashSet<>();
        for (Compra c : lista) {
            ids.add(c.getCreadoPorId());
            if (c.getAnuladaPorId() != null) {
                ids.add(c.getAnuladaPorId());
            }
        }
        return ids;
    }

    private static Specification<Compra> especificacion(FiltroCompras filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.proveedorId() != null) {
                condiciones.add(cb.equal(raiz.get("proveedorId"), filtro.proveedorId()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
            }
            LineasDocumento.rangoFechas(condiciones, cb, raiz.get("fecha"), filtro.desde(), filtro.hasta());
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
