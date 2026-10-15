/*
 * Nombre:                 VentaServicio.java
 * Descripción:            Confirmación de ventas con pago en efectivo, consulta y anulación (RF-VEN, RF-PAG).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.ventas.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.servicio.MovimientoInventarioServicio;
import com.ferrestock.terceros.entidad.Tercero;
import com.ferrestock.terceros.servicio.FiltroTerceros;
import com.ferrestock.terceros.servicio.TerceroServicio;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
import com.ferrestock.ventas.dto.respuesta.DevolvibleResponse;
import com.ferrestock.ventas.dto.respuesta.VentaResponse;
import com.ferrestock.ventas.dto.solicitud.VentaCrearRequest;
import com.ferrestock.ventas.entidad.Venta;
import com.ferrestock.ventas.entidad.VentaLinea;
import com.ferrestock.ventas.repositorio.VentaRepositorio;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ventas (CU-05, CU-06). Venta y pago se confirman en una sola transacción (DP-10): los productos se bloquean en
 * orden de id, se verifican las existencias y se descuentan, de modo que dos ventas simultáneas nunca dejan saldo
 * negativo (RN-01, CA-VEN-03).
 */
@Service
@RequiredArgsConstructor
public class VentaServicio {

    /** Único medio de pago aceptado (RF-PAG-002). */
    public static final String EFECTIVO = "EFECTIVO";

    private final VentaRepositorio ventas;
    private final ProductoServicio productos;
    private final TerceroServicio terceros;
    private final MovimientoInventarioServicio movimientos;
    private final ConsecutivoServicio consecutivos;
    private final UsuarioServicio usuarios;
    private final AuditoriaServicio auditoria;
    private final Clock reloj;

    /**
     * Confirma una venta con su pago en efectivo.
     *
     * @param solicitud cliente, líneas y pago
     * @return venta confirmada con total, cambio y consecutivo
     * @throws NegocioException si el cliente, un producto, una cantidad, las existencias o el pago no son válidos
     */
    @Transactional
    public VentaResponse registrar(VentaCrearRequest solicitud) {
        UsuarioAutenticado actor = UsuarioActual.exigir();
        Tercero cliente = terceros.exigirParaDocumento(solicitud.clienteId(), FiltroTerceros.RolTercero.CLIENTE, "clienteId");
        if (!EFECTIVO.equalsIgnoreCase(solicitud.pago().medio().trim())) {
            throw NegocioException.enCampo(CodigoError.MEDIO_PAGO_NO_PERMITIDO, "pago.medio");
        }
        List<VentaCrearRequest.Linea> lineas = solicitud.lineas();
        LineasDocumento.exigirSinRepetidos(lineas.stream().map(VentaCrearRequest.Linea::productoId).toList());
        Map<Long, Producto> bloqueados = productos.bloquearParaMovimiento(
            lineas.stream().map(VentaCrearRequest.Linea::productoId).toList());

        Instant ahora = reloj.instant();
        Venta venta = Venta.builder().clienteId(cliente.getId()).fecha(ahora).creadoPorId(actor.getId()).creadoEn(ahora).build();
        BigDecimal total = Montos.dinero(BigDecimal.ZERO);
        for (int i = 0; i < lineas.size(); i++) {
            Producto producto = bloqueados.get(lineas.get(i).productoId());
            if (producto.getEstado() == EstadoProducto.INACTIVO) {
                throw NegocioException.enCampo(CodigoError.PRODUCTO_INACTIVO, LineasDocumento.campo(i, "productoId"));
            }
            BigDecimal cantidad = movimientos.validarCantidad(producto, lineas.get(i).cantidad(), LineasDocumento.campo(i, "cantidad"));
            if (producto.getExistencias().compareTo(cantidad) < 0) {
                throw NegocioException.enCampo(CodigoError.EXISTENCIAS_INSUFICIENTES, LineasDocumento.campo(i, "cantidad"),
                    producto.getCodigo(), producto.getExistencias().stripTrailingZeros().toPlainString());
            }
            BigDecimal subtotal = Montos.subtotal(cantidad, producto.getPrecioVenta());
            total = total.add(subtotal);
            venta.agregar(VentaLinea.builder().productoId(producto.getId()).cantidad(cantidad)
                .precioUnitario(producto.getPrecioVenta()).costoUnitario(producto.getCostoPromedio()).subtotal(subtotal).build());
        }
        BigDecimal recibido = Montos.dinero(solicitud.pago().valorRecibido());
        if (recibido.compareTo(total) < 0) {
            throw NegocioException.enCampo(CodigoError.PAGO_INSUFICIENTE, "pago.valorRecibido", total.toPlainString());
        }
        venta.setTotal(total);
        venta.setValorRecibido(recibido);
        venta.setCambio(recibido.subtract(total));
        venta.setConsecutivo(consecutivos.siguiente(TipoDocumentoComercial.VENTA));
        Venta guardada = ventas.saveAndFlush(venta);
        for (VentaLinea linea : guardada.getLineas()) {
            movimientos.registrarSalida(bloqueados.get(linea.getProductoId()), linea.getCantidad(), TipoMovimiento.VENTA,
                TipoDocumentoComercial.VENTA, guardada.getId());
        }
        auditoria.registrar(AccionAuditoria.VENTA_REGISTRADA, "Venta " + guardada.getConsecutivo() + " a "
            + cliente.getNombre() + " por " + total.toPlainString() + " (recibido " + recibido.toPlainString() + ", cambio "
            + guardada.getCambio().toPlainString() + ")");
        return respuesta(guardada);
    }

    /**
     * @param filtro cliente, fechas y estado
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @return ventas de la más reciente a la más antigua, sin líneas
     */
    @Transactional(readOnly = true)
    public PaginaResponse<VentaResponse> listar(FiltroVentas filtro, int pagina, int tamano) {
        Page<Venta> resultado = ventas.findAll(especificacion(filtro),
            PaginaResponse.solicitud(pagina, tamano, Sort.by(Sort.Order.desc("id"))));
        Map<Long, String> clientes = terceros.nombresPorId(resultado.map(Venta::getClienteId).toSet());
        Map<Long, String> nombres = usuarios.nombresPorId(idsUsuarios(resultado.getContent()));
        return PaginaResponse.de(resultado.map(v -> respuesta(v, clientes, nombres, null)));
    }

    /**
     * @param id venta
     * @return la venta con líneas y pago
     */
    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return respuesta(ventas.conLineas(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO)));
    }

    /**
     * Anula una venta (solo ADMIN): las cantidades reingresan al costo histórico de cada línea y ponderan el CPP.
     * Se rechaza si alguna línea tiene devoluciones (DP-20).
     *
     * @param id     venta
     * @param motivo motivo de la anulación
     * @return venta anulada
     */
    @Transactional
    public VentaResponse anular(Long id, String motivo) {
        UsuarioAutenticado actor = UsuarioActual.exigir();
        if (!actor.esAdmin()) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        Venta venta = ventas.bloquear(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
        if (venta.getEstado() == EstadoDocumento.ANULADA) {
            throw new NegocioException(CodigoError.DOCUMENTO_ANULADO);
        }
        if (venta.getLineas().stream().anyMatch(l -> l.getCantidadDevuelta().signum() > 0)) {
            throw new NegocioException(CodigoError.VENTA_CON_DEVOLUCIONES);
        }
        Map<Long, Producto> bloqueados = productos.bloquearParaMovimiento(
            venta.getLineas().stream().map(VentaLinea::getProductoId).toList());
        for (VentaLinea linea : venta.getLineas()) {
            movimientos.registrarEntrada(bloqueados.get(linea.getProductoId()), linea.getCantidad(), linea.getCostoUnitario(),
                TipoMovimiento.ANULACION_VENTA, true, TipoDocumentoComercial.VENTA, venta.getId());
        }
        venta.setEstado(EstadoDocumento.ANULADA);
        venta.setMotivoAnulacion(motivo.trim());
        venta.setAnuladaPorId(actor.getId());
        venta.setAnuladaEn(reloj.instant());
        Venta guardada = ventas.saveAndFlush(venta);
        auditoria.registrar(AccionAuditoria.VENTA_ANULADA, "Venta " + venta.getConsecutivo() + " anulada. Motivo: "
            + venta.getMotivoAnulacion());
        return respuesta(guardada);
    }

    /**
     * Venta bloqueada para registrar una devolución sobre ella (la usa el módulo de devoluciones).
     *
     * @param id venta
     * @return la venta con sus líneas, bloqueada hasta el fin de la transacción
     * @throws NegocioException {@code NO_ENCONTRADO} en el campo {@code ventaId}
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Venta bloquearParaDevolucion(Long id) {
        return ventas.bloquear(id).orElseThrow(() -> NegocioException.enCampo(CodigoError.NO_ENCONTRADO, "ventaId"));
    }

    /**
     * Cuánto queda por devolver de cada línea de una venta registrada (ayuda al formulario de devoluciones).
     *
     * @param id venta
     * @return líneas con lo vendido, lo devuelto y lo disponible
     * @throws NegocioException {@code NO_ENCONTRADO} o {@code VENTA_NO_CORRESPONDE} si está anulada
     */
    @Transactional(readOnly = true)
    public List<DevolvibleResponse> devolvible(Long id) {
        Venta venta = ventas.conLineas(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
        if (venta.getEstado() == EstadoDocumento.ANULADA) {
            throw new NegocioException(CodigoError.VENTA_NO_CORRESPONDE);
        }
        Map<Long, Producto> porId = productos.porIds(venta.getLineas().stream().map(VentaLinea::getProductoId).toList());
        return venta.getLineas().stream().map(l -> {
            Producto p = porId.get(l.getProductoId());
            return new DevolvibleResponse(l.getId(), l.getProductoId(), p.getCodigo(), p.getNombre(), p.isPermiteFraccion(),
                l.getCantidad(), l.getCantidadDevuelta(), Montos.cantidad(l.getCantidad().subtract(l.getCantidadDevuelta())));
        }).toList();
    }

    /**
     * @param ids ventas
     * @return consecutivos por id, para mostrar la venta de origen de una devolución
     */
    @Transactional(readOnly = true)
    public Map<Long, String> consecutivosPorId(Collection<Long> ids) {
        return ventas.findAllById(ids).stream().collect(Collectors.toMap(Venta::getId, Venta::getConsecutivo));
    }

    private VentaResponse respuesta(Venta venta) {
        Map<Long, String> clientes = terceros.nombresPorId(List.of(venta.getClienteId()));
        Map<Long, String> nombres = usuarios.nombresPorId(idsUsuarios(List.of(venta)));
        boolean admin = UsuarioActual.obtener().map(UsuarioAutenticado::esAdmin).orElse(false);
        Map<Long, Producto> porId = productos.porIds(venta.getLineas().stream().map(VentaLinea::getProductoId).toList());
        List<VentaResponse.Linea> lineas = venta.getLineas().stream().map(l -> {
            Producto p = porId.get(l.getProductoId());
            return new VentaResponse.Linea(l.getId(), l.getProductoId(), p.getCodigo(), p.getNombre(), l.getCantidad(),
                l.getPrecioUnitario(), l.getSubtotal(), l.getCantidadDevuelta(), admin ? l.getCostoUnitario() : null);
        }).toList();
        return respuesta(venta, clientes, nombres, lineas);
    }

    private static VentaResponse respuesta(Venta v, Map<Long, String> clientes, Map<Long, String> nombres,
                                           List<VentaResponse.Linea> lineas) {
        return new VentaResponse(v.getId(), v.getConsecutivo(), v.getFecha(), v.getClienteId(), clientes.get(v.getClienteId()),
            v.getTotal(), v.getMedioPago(), v.getValorRecibido(), v.getCambio(), v.getEstado().name(), v.getMotivoAnulacion(),
            v.getAnuladaPorId() == null ? null : nombres.get(v.getAnuladaPorId()), v.getAnuladaEn(),
            nombres.get(v.getCreadoPorId()), lineas);
    }

    private static Set<Long> idsUsuarios(List<Venta> lista) {
        Set<Long> ids = new HashSet<>();
        for (Venta v : lista) {
            ids.add(v.getCreadoPorId());
            if (v.getAnuladaPorId() != null) {
                ids.add(v.getAnuladaPorId());
            }
        }
        return ids;
    }

    private static Specification<Venta> especificacion(FiltroVentas filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.clienteId() != null) {
                condiciones.add(cb.equal(raiz.get("clienteId"), filtro.clienteId()));
            }
            if (filtro.estado() != null) {
                condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
            }
            LineasDocumento.rangoFechas(condiciones, cb, raiz.get("fecha"), filtro.desde(), filtro.hasta());
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
