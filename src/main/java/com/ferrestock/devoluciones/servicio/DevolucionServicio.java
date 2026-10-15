/*
 * Nombre:                 DevolucionServicio.java
 * Descripción:            Registro, validación y consulta de devoluciones de clientes (RF-DEV, RF-COS-003).
 * Fecha de creación:      2026-10-15
 * Fecha de actualización: 2026-10-15
 * Autores:                Ana Sofía Nagles García, María Mercedes Rincón Valencia, Juan David Camacho Tapiero
 * Versión:                0.9.0
 */
package com.ferrestock.devoluciones.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ferrestock.auditoria.entidad.AccionAuditoria;
import com.ferrestock.auditoria.servicio.AuditoriaServicio;
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
import com.ferrestock.devoluciones.dto.respuesta.DevolucionResponse;
import com.ferrestock.devoluciones.dto.solicitud.DevolucionCrearRequest;
import com.ferrestock.devoluciones.entidad.Devolucion;
import com.ferrestock.devoluciones.entidad.DevolucionLinea;
import com.ferrestock.devoluciones.entidad.ResultadoLinea;
import com.ferrestock.devoluciones.repositorio.DevolucionRepositorio;
import com.ferrestock.inventario.entidad.TipoMovimiento;
import com.ferrestock.inventario.servicio.MovimientoInventarioServicio;
import com.ferrestock.terceros.entidad.Tercero;
import com.ferrestock.terceros.servicio.FiltroTerceros;
import com.ferrestock.terceros.servicio.TerceroServicio;
import com.ferrestock.usuarios.servicio.UsuarioServicio;
import com.ferrestock.ventas.entidad.Venta;
import com.ferrestock.ventas.entidad.VentaLinea;
import com.ferrestock.ventas.servicio.VentaServicio;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devoluciones (CU-07). En una sola transacción se registra la solicitud, se validan sus líneas y las aceptadas
 * reingresan al inventario (03_inventario_y_costos.md, sección 6):
 * <ul>
 *   <li>Con venta: al costo histórico de la línea vendida, ponderando el CPP; se acumula la cantidad devuelta bajo
 *       el bloqueo de la venta.</li>
 *   <li>Sin venta: al CPP vigente, sin cambiarlo; queda auditada como {@code DEVOLUCION_SIN_VENTA}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DevolucionServicio {

    private final DevolucionRepositorio devoluciones;
    private final VentaServicio ventas;
    private final ProductoServicio productos;
    private final TerceroServicio terceros;
    private final MovimientoInventarioServicio movimientos;
    private final ConsecutivoServicio consecutivos;
    private final UsuarioServicio usuarios;
    private final AuditoriaServicio auditoria;
    private final Clock reloj;

    /**
     * Registra y valida una devolución.
     *
     * @param solicitud cliente, venta opcional, motivo y líneas con su resultado
     * @return devolución registrada
     */
    @Transactional
    public DevolucionResponse registrar(DevolucionCrearRequest solicitud) {
        UsuarioAutenticado actor = UsuarioActual.exigir();
        Tercero cliente = terceros.exigirParaDocumento(solicitud.clienteId(), FiltroTerceros.RolTercero.CLIENTE, "clienteId");
        List<DevolucionCrearRequest.Linea> lineas = solicitud.lineas();
        LineasDocumento.exigirSinRepetidos(lineas.stream().map(DevolucionCrearRequest.Linea::productoId).toList());

        Venta venta = null;
        if (solicitud.ventaId() != null) {
            venta = ventas.bloquearParaDevolucion(solicitud.ventaId());
            if (!venta.getClienteId().equals(cliente.getId()) || venta.getEstado() != EstadoDocumento.REGISTRADA) {
                throw NegocioException.enCampo(CodigoError.VENTA_NO_CORRESPONDE, "ventaId");
            }
        }
        // Los productos inactivos se aceptan (DP-09): la devolución es consecuencia de una venta pasada
        Map<Long, Producto> bloqueados = productos.bloquearParaMovimiento(
            lineas.stream().map(DevolucionCrearRequest.Linea::productoId).toList());

        Instant ahora = reloj.instant();
        Devolucion devolucion = Devolucion.builder().clienteId(cliente.getId()).ventaId(venta == null ? null : venta.getId())
            .motivo(solicitud.motivo().trim()).fecha(ahora).validadaPorId(actor.getId()).creadoPorId(actor.getId())
            .creadoEn(ahora).build();
        List<VentaLinea> lineasVenta = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            DevolucionCrearRequest.Linea linea = lineas.get(i);
            Producto producto = bloqueados.get(linea.productoId());
            BigDecimal cantidad = movimientos.validarCantidad(producto, linea.cantidad(), LineasDocumento.campo(i, "cantidad"));
            VentaLinea vendida = venta == null ? null : lineaDeVenta(venta, producto.getId(), i);
            boolean aceptada = linea.resultado() == ResultadoLinea.ACEPTADA;
            BigDecimal costo = null;
            if (aceptada) {
                if (vendida != null) {
                    BigDecimal acumulada = vendida.getCantidadDevuelta().add(cantidad);
                    if (acumulada.compareTo(vendida.getCantidad()) > 0) {
                        throw NegocioException.enCampo(CodigoError.DEVOLUCION_EXCEDE_VENDIDO, LineasDocumento.campo(i, "cantidad"),
                            producto.getCodigo(), vendida.getCantidad().stripTrailingZeros().toPlainString(),
                            vendida.getCantidadDevuelta().stripTrailingZeros().toPlainString());
                    }
                    vendida.setCantidadDevuelta(Montos.cantidad(acumulada));
                    costo = vendida.getCostoUnitario();
                } else {
                    costo = producto.getCostoPromedio();
                }
            }
            lineasVenta.add(vendida);
            devolucion.agregar(DevolucionLinea.builder().productoId(producto.getId())
                .ventaLineaId(vendida == null ? null : vendida.getId()).cantidad(cantidad).resultado(linea.resultado())
                .costoUnitario(costo).build());
        }
        devolucion.setConsecutivo(consecutivos.siguiente(TipoDocumentoComercial.DEVOLUCION));
        Devolucion guardada = devoluciones.saveAndFlush(devolucion);
        for (int i = 0; i < guardada.getLineas().size(); i++) {
            DevolucionLinea linea = guardada.getLineas().get(i);
            if (linea.getResultado() == ResultadoLinea.ACEPTADA) {
                // Con venta pondera al costo histórico; sin venta entra al CPP vigente y no lo cambia (DP-08)
                movimientos.registrarEntrada(bloqueados.get(linea.getProductoId()), linea.getCantidad(), linea.getCostoUnitario(),
                    TipoMovimiento.DEVOLUCION, lineasVenta.get(i) != null, TipoDocumentoComercial.DEVOLUCION, guardada.getId());
            }
        }
        long aceptadas = guardada.getLineas().stream().filter(l -> l.getResultado() == ResultadoLinea.ACEPTADA).count();
        String resumen = "Devolución " + guardada.getConsecutivo() + " de " + cliente.getNombre()
            + (venta == null ? " sin venta vinculada" : " sobre la venta " + venta.getConsecutivo()) + ": " + aceptadas
            + " línea(s) aceptada(s) y " + (guardada.getLineas().size() - aceptadas) + " rechazada(s), validadas por "
            + actor.getNombreUsuario() + ". Motivo: " + guardada.getMotivo();
        auditoria.registrar(venta == null ? AccionAuditoria.DEVOLUCION_SIN_VENTA : AccionAuditoria.DEVOLUCION_REGISTRADA,
            resumen.length() > 500 ? resumen.substring(0, 500) : resumen);
        return respuesta(guardada, venta == null ? null : venta.getConsecutivo());
    }

    /**
     * @param filtro cliente, venta y fechas
     * @param pagina número de página
     * @param tamano tamaño (máximo 100)
     * @return devoluciones de la más reciente a la más antigua, sin líneas
     */
    @Transactional(readOnly = true)
    public PaginaResponse<DevolucionResponse> listar(FiltroDevoluciones filtro, int pagina, int tamano) {
        Page<Devolucion> resultado = devoluciones.findAll(especificacion(filtro),
            PaginaResponse.solicitud(pagina, tamano, Sort.by(Sort.Order.desc("id"))));
        Map<Long, String> clientes = terceros.nombresPorId(resultado.map(Devolucion::getClienteId).toSet());
        Map<Long, String> nombres = usuarios.nombresPorId(resultado.map(Devolucion::getValidadaPorId).toSet());
        Map<Long, String> consecutivosVenta = ventas.consecutivosPorId(resultado.getContent().stream()
            .map(Devolucion::getVentaId).filter(java.util.Objects::nonNull).toList());
        return PaginaResponse.de(resultado.map(d -> respuesta(d, clientes, nombres,
            d.getVentaId() == null ? null : consecutivosVenta.get(d.getVentaId()), null)));
    }

    /**
     * @param id devolución
     * @return la devolución con sus líneas
     */
    @Transactional(readOnly = true)
    public DevolucionResponse obtener(Long id) {
        Devolucion devolucion = devoluciones.conLineas(id).orElseThrow(() -> new NegocioException(CodigoError.NO_ENCONTRADO));
        String venta = devolucion.getVentaId() == null ? null
            : ventas.consecutivosPorId(List.of(devolucion.getVentaId())).get(devolucion.getVentaId());
        return respuesta(devolucion, venta);
    }

    private static VentaLinea lineaDeVenta(Venta venta, Long productoId, int indice) {
        Optional<VentaLinea> linea = venta.getLineas().stream().filter(l -> l.getProductoId().equals(productoId)).findFirst();
        return linea.orElseThrow(() -> NegocioException.enCampo(CodigoError.VENTA_NO_CORRESPONDE,
            LineasDocumento.campo(indice, "productoId")));
    }

    private DevolucionResponse respuesta(Devolucion devolucion, String ventaConsecutivo) {
        Map<Long, String> clientes = terceros.nombresPorId(List.of(devolucion.getClienteId()));
        Map<Long, String> nombres = usuarios.nombresPorId(Set.of(devolucion.getValidadaPorId()));
        boolean admin = UsuarioActual.obtener().map(UsuarioAutenticado::esAdmin).orElse(false);
        Map<Long, Producto> porId = productos.porIds(devolucion.getLineas().stream().map(DevolucionLinea::getProductoId).toList());
        List<DevolucionResponse.Linea> lineas = devolucion.getLineas().stream().map(l -> {
            Producto p = porId.get(l.getProductoId());
            return new DevolucionResponse.Linea(l.getId(), l.getProductoId(), p.getCodigo(), p.getNombre(), l.getCantidad(),
                l.getResultado().name(), admin ? l.getCostoUnitario() : null);
        }).toList();
        return respuesta(devolucion, clientes, nombres, ventaConsecutivo, lineas);
    }

    private static DevolucionResponse respuesta(Devolucion d, Map<Long, String> clientes, Map<Long, String> nombres,
                                                String ventaConsecutivo, List<DevolucionResponse.Linea> lineas) {
        int aceptadas = (int) d.getLineas().stream().filter(l -> l.getResultado() == ResultadoLinea.ACEPTADA).count();
        return new DevolucionResponse(d.getId(), d.getConsecutivo(), d.getFecha(), d.getClienteId(),
            clientes.get(d.getClienteId()), d.getVentaId(), ventaConsecutivo, d.getMotivo(), nombres.get(d.getValidadaPorId()),
            aceptadas, d.getLineas().size() - aceptadas, lineas);
    }

    private static Specification<Devolucion> especificacion(FiltroDevoluciones filtro) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.clienteId() != null) {
                condiciones.add(cb.equal(raiz.get("clienteId"), filtro.clienteId()));
            }
            if (filtro.ventaId() != null) {
                condiciones.add(cb.equal(raiz.get("ventaId"), filtro.ventaId()));
            }
            LineasDocumento.rangoFechas(condiciones, cb, raiz.get("fecha"), filtro.desde(), filtro.hasta());
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
